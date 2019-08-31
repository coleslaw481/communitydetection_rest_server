package org.ndexbio.communitydetection.rest.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.ndexbio.communitydetection.rest.engine.util.DockerCommunityDetectionRunner;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithms;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResultStatus;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResult;
import org.ndexbio.communitydetection.rest.model.ServerStatus;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;

import org.ndexbio.communitydetection.rest.services.CommunityDetectionHttpServletDispatcher;
import org.ndexbio.communitydetection.rest.services.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs enrichment 
 * @author churas
 */
public class CommunityDetectionEngineImpl implements CommunityDetectionEngine {

    public static final String CDREQUEST_JSON_FILE = "cdrequest.json";
    
    public static final String CDRESULT_JSON_FILE = "cdresult.json";
    
    static Logger _logger = LoggerFactory.getLogger(CommunityDetectionEngineImpl.class);

    private String _taskDir;
    private boolean _shutdown;
    private ExecutorService _executorService;
    private ConcurrentHashMap<String, Future> _futureTaskMap;
    private AtomicInteger _completedTasks;
    private AtomicInteger _queuedTasks;
    private AtomicInteger _canceledTasks;
    private HashMap<String, String> _algoToDockerMap;
    private String _dockerCmd;
        
    /**
     * This should be a map of <query UUID> => EnrichmentQueryResults object
     */
    private ConcurrentHashMap<String, CommunityDetectionResult> _results;

    private long _threadSleep = 10;
    
    public CommunityDetectionEngineImpl(ExecutorService es,
            final String taskDir,
            final String dockerCmd,
            final HashMap<String, String> algoToDockerMap){
        _executorService = es;
        _shutdown = false;
        _futureTaskMap = new ConcurrentHashMap<>();
        _taskDir = taskDir;
        _dockerCmd = dockerCmd;
        _algoToDockerMap = algoToDockerMap;
        _results = new ConcurrentHashMap<>();
        _completedTasks = new AtomicInteger(0);
        _queuedTasks = new AtomicInteger(0);
        _canceledTasks = new AtomicInteger(0);
    }
    
    /**
     * Sets milliseconds thread should sleep if no work needs to be done.
     * @param sleepTime 
     */
    public void updateThreadSleepTime(long sleepTime){
        _threadSleep = sleepTime;
    }

    protected void threadSleep(){
        try {
            Thread.sleep(_threadSleep);
        }
        catch(InterruptedException ie){

        }
    }
    
    /**
     * Processes any query tasks, looping until {@link #shutdown()} is invoked
     */
    @Override
    public void run() {
        while(_shutdown == false){
            Future f;
            String taskId;
            int queuedCount = 0;
            Iterator<String> idItr = _futureTaskMap.keySet().iterator();
            while(idItr.hasNext()){
                taskId = idItr.next();
                
                f = _futureTaskMap.get(taskId);
                if (f == null){
                    continue;
                }
                if (f.isCancelled()){
                    _futureTaskMap.remove(taskId);
                    _canceledTasks.incrementAndGet();
                } else if (f.isDone()){
                    _logger.debug("Found a completed or failed task");
                    try {
                        CommunityDetectionResult cdr = (CommunityDetectionResult) f.get();
                        saveCommunityDetectionResultToFilesystem(cdr);
                        _completedTasks.incrementAndGet();
                    } catch (InterruptedException ex) {
                        _logger.error("Got interrupted exception", ex);
                    } catch (ExecutionException ex) {
                        _logger.error("Got execution exception", ex);
                    } catch (CancellationException ex){
                        _logger.error("Got cancellation exception", ex);
                    }
                    _futureTaskMap.remove(taskId);
                } else {
                    queuedCount++;
                }
            }
            if (_queuedTasks.get() != queuedCount){
                _queuedTasks.set(queuedCount);
            }
            threadSleep();
        }
        _logger.debug("Shutdown was invoked");
        logServerStatus(null);
    }

    @Override
    public void shutdown() {
        _shutdown = true;
    }
    
    /**
     * Calls {@link #getServerStatus() } and dumps the status of the server as
     * a JSON string to the info level of the logger for this class
     */
    protected void logServerStatus(final ServerStatus ss){
        try {
            final ServerStatus sStat;
            if (ss != null){
                sStat = ss;
            } else {
                sStat = this.getServerStatus();
            }
           
           ObjectMapper mapper = new ObjectMapper();
           _logger.info("ServerStatus: " + mapper.writeValueAsString(sStat));
        }catch(Exception ex){
            _logger.error("error trying to log server status", ex);
        }
    }
    
    protected String getCommunityDetectionResultFilePath(final String id){
        return this._taskDir + File.separator + id + File.separator + CommunityDetectionEngineImpl.CDRESULT_JSON_FILE;
    }

    protected void saveCommunityDetectionResultToFilesystem(final CommunityDetectionResult cdr){
        if (cdr == null){
            _logger.error("Received a null result, unable to save");
            return;
        }
        
        File destFile = new File(getCommunityDetectionResultFilePath(cdr.getId()));
        ObjectMapper mappy = new ObjectMapper();
        try (FileOutputStream out = new FileOutputStream(destFile)){
            mappy.writeValue(out, cdr);
        } catch(IOException io){
            _logger.error("Caught exception writing " + destFile.getAbsolutePath(), io);
        }
        _results.remove(cdr.getId());
    }
    
    protected CommunityDetectionResult getCommunityDetectionResultFromDbOrFilesystem(final String id){
        ObjectMapper mappy = new ObjectMapper();
        File cdrFile = new File(getCommunityDetectionResultFilePath(id));
        if (cdrFile.isFile() == false){
            _logger.error(cdrFile.getAbsolutePath() + " is not a file");
            return _results.get(id);
        }
        try {
            return mappy.readValue(cdrFile, CommunityDetectionResult.class);
        }catch(IOException io){
            _logger.error("Caught exception trying to load " + cdrFile.getAbsolutePath(), io);
        }
        return _results.get(id);
    }
    
    @Override
    public String request(CommunityDetectionRequest request) throws CommunityDetectionException {
        if (request == null){ 
            throw new CommunityDetectionException("Request is null");
        }
        if (request.getAlgorithm() == null){
            throw new CommunityDetectionException("No algorithm specified");
        }
        if (request.getData()== null){
            throw new CommunityDetectionException("data is null");
        }
        String id = UUID.randomUUID().toString();

        CommunityDetectionResult cdr = new CommunityDetectionResult(System.currentTimeMillis());
        cdr.setStatus(CommunityDetectionResult.SUBMITTED_STATUS);
        cdr.setId(id);
        _results.put(id, cdr);
        
        if (_algoToDockerMap.containsKey(request.getAlgorithm()) == false){
            throw new CommunityDetectionException(request.getAlgorithm() + " is not a valid algorithm");
        }
        
        String dockerImage = _algoToDockerMap.get(request.getAlgorithm());
        try {
            DockerCommunityDetectionRunner task = new DockerCommunityDetectionRunner(id, request, cdr.getStartTime(),
            _taskDir, _dockerCmd, dockerImage, Configuration.getInstance().getAlgorithmTimeOut(),
            TimeUnit.SECONDS);
            _futureTaskMap.put(id, _executorService.submit(task));
            return id;
        } catch(Exception ex){
            throw new CommunityDetectionException(ex.getMessage());
        }
    }

    @Override
    public CommunityDetectionResult getResult(String id) throws CommunityDetectionException {
        CommunityDetectionResult cdr = getCommunityDetectionResultFromDbOrFilesystem(id);
        if (cdr == null){
            throw new CommunityDetectionException("No task with " + id + " found");
        }
        return cdr;
    }

    @Override
    public CommunityDetectionResultStatus getStatus(String id) throws CommunityDetectionException {
        CommunityDetectionResult cdr = getCommunityDetectionResultFromDbOrFilesystem(id);
        if (cdr == null){
            throw new CommunityDetectionException("No task with " + id + " found");
        }
        return new CommunityDetectionResultStatus(cdr);
    }

    @Override
    public void delete(String id) throws CommunityDetectionException {
        _logger.debug("Deleting task " + id);
        if (_results.containsKey(id) == true){
            _results.remove(id);
        }
        Future f = _futureTaskMap.get(id);
        if (f != null){
            _logger.info("Canceling task: " + id + " result of cancel(): " +
                    Boolean.toString(f.cancel(true)));
        }
        File thisTaskDir = new File(this._taskDir + File.separator + id);
        if (thisTaskDir.exists() == false){
            return;
        }
        _logger.debug("Attempting to delete task from filesystem: " + thisTaskDir.getAbsolutePath());
        if (FileUtils.deleteQuietly(thisTaskDir) == false){
            _logger.error("There was a problem deleting the directory: " + thisTaskDir.getAbsolutePath());
        }
    }

    @Override
    public CommunityDetectionAlgorithms getAlgorithms() throws CommunityDetectionException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public ServerStatus getServerStatus() throws CommunityDetectionException {
        try {
            String version = "unknown";
            ServerStatus sObj = new ServerStatus();
            sObj.setStatus(ServerStatus.OK_STATUS);
            sObj.setRestVersion(CommunityDetectionHttpServletDispatcher.getVersion());
            OperatingSystemMXBean omb = ManagementFactory.getOperatingSystemMXBean();
            float unknown = (float)-1;
            float load = (float)omb.getSystemLoadAverage();
            sObj.setLoad(Arrays.asList(load, unknown, unknown));
            File taskDir = new File(this._taskDir);
            sObj.setPcDiskFull(100-(int)Math.round(((double)taskDir.getFreeSpace()/(double)taskDir.getTotalSpace())*100));
            sObj.setQueuedTasks(_queuedTasks.get());
            sObj.setCompletedTasks(_completedTasks.get());
            sObj.setCanceledTasks(_canceledTasks.get());
            logServerStatus(sObj);
            return sObj;
        } catch(Exception ex){
            _logger.error("ServerStatus error", ex);
            throw new CommunityDetectionException("Exception raised when getting ServerStatus: " + ex.getMessage());
        }
    }
}
