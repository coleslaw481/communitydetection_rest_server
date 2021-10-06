package org.ndexbio.communitydetection.rest.engine.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResult;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;
import org.ndexbio.communitydetection.rest.services.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs algorithm via commandline
 * @author churas
 */
public class DockerCommunityDetectionRunner implements Callable {

    
    static Logger _logger = LoggerFactory.getLogger(DockerCommunityDetectionRunner.class);

    public static final String INPUT_FILE = "input.txt";
    public static final String STD_OUT_FILE = "stdout.txt";
    public static final String STD_ERR_FILE = "stderr.txt";
    public static final String CMD_RUN_FILE = "cmdrun.sh";
    
    private String _id;
    private CommunityDetectionRequest _cdr;
    private String _dockerCmd;
    private String _dockerImage;
    private Map<String, String> _customParameters;
    private String _taskDir;
    private String _workDir;
    private long _startTime;
    private long _timeOut;
    private TimeUnit _timeUnit;
    private String _mountOptions;
 
    private CommandLineRunner _runner;
    
    /**
     * Constructor 
     * @param id id of task (should be a 37 char uuid string)
     * @param cdr The request to process
     * @param startTime Time task started in ms since epoch (1969)
     * @param taskDir Base directory for tasks (this task will be put into taskDir/id)
     * @param dockerCmd Command to run docker (/usr/bin/docker /bin/docker etc..)
     * @param dockerImage Docker image to run (hello-world)
     * @param timeOut Any task exceeding this time (in unit set by unit) will be killed
     * @param unit Unit to use for timeout
     * @throws Exception If there is an issue writing the input data from the cdr object
     */
    public DockerCommunityDetectionRunner(final String id,
            final CommunityDetectionRequest cdr, final long startTime, final String taskDir,
            final String dockerCmd, final String dockerImage,
            final Map<String, String> customParameters,
            final long timeOut,
            final TimeUnit unit,
            final String mountOptions) throws Exception{
        _id = id;
        _cdr = cdr;
        _dockerCmd = dockerCmd;
        _dockerImage = dockerImage;
        _customParameters = customParameters;
        _startTime = startTime;
        _taskDir = taskDir;
        _workDir = _taskDir + File.separator + _id;
        _timeOut = timeOut;
        _timeUnit = unit;
        if (mountOptions != null){
            _mountOptions = mountOptions;
        }
        else {
            _mountOptions = "";
        }

        writeInputFile();
       
        _runner = new CommandLineRunnerImpl();
        
    }
    
    /**
     * For testing, lets one set alternate command line runner
     * @param clr 
     */
    protected void setAlternateCommandLineRunner(CommandLineRunner clr){
        _runner = clr;
    }
    
    /**
     * Writes contents {@link org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest#getData()}
     * set via {@link #DockerCommunityDetectionRunner(java.lang.String, org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest, long, java.lang.String, java.lang.String, java.lang.String, long, java.util.concurrent.TimeUnit) } 
     * to file which is assumed to be either a {@link com.fasterxml.jackson.databind.node.TextNode}
     * or a {@link com.fasterxml.databind.node.JsonNode}
     * @return full path to input file as String
     * @throws CommunityDetectionException If there was an issue creating task directories
     * @throws IOException If there was IO error writing the data to a file
     */
    protected String writeInputFile() throws CommunityDetectionException, IOException {
        File workDir = new File(_workDir);
        
        if (workDir.isDirectory() == false){
            if (workDir.mkdirs() == false){
                throw new CommunityDetectionException("Unable to create directory: " + _workDir);
            }
        }
        File destFile = getInputFile();
        if (_cdr.getData()instanceof TextNode){
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(destFile))){
                bw.write(_cdr.getData().asText());
            }
        }
        else {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(destFile, _cdr.getData()); 
        }
        return destFile.getAbsolutePath();
    }
    
    /**
     * This method generates a {@link java.io.File} object pointing to standard 
     * out file generated by {@link #call()}
     * which may or may not exist yet.
     * @return 
     */
    protected File getStandardOutFile(){
        return new File(_workDir + File.separator + STD_OUT_FILE);
    }
    
    /**
     * This method generates a {@link java.io.File} object pointing to 
     * standard error file generated by {@link #call()}
     * which may or may not exist yet.
     * @return 
     */
    protected File getStandardErrorFile(){
        return new File(_workDir + File.separator + STD_ERR_FILE);
    }
    
    /**
     * This method generates a {@link java.io.File} object pointing to input 
     * file generated by {@link #writeInputFile() ()}
     * which may or may not exist yet.
     * @return 
     */
    protected File getInputFile(){
        return new File(_workDir + File.separator + INPUT_FILE);
    }

    /**
     * This method generates a {@link java.io.File} object pointing to a 
     * file that contains the
     * actual command run by the command line program which is created by 
     * {@link #call()}
     * @return 
     */
    protected File getCommandRunFile(){
        return new File(_workDir + File.separator + CMD_RUN_FILE);
    }
    
    /**
     * Creates a {@link org.ndexbio.communitydetection.rest.model.CommunityDetectionResult}
     * object with id, status, and start time set.
     *
     * @return 
     */
    protected CommunityDetectionResult createCommunityDetectionResult(){
        CommunityDetectionResult cdr = new CommunityDetectionResult();
        cdr.setId(_id);
        cdr.setProgress(0);
        cdr.setStartTime(_startTime);
        cdr.setStatus(CommunityDetectionResult.PROCESSING_STATUS);
        return cdr;
    }
    
    /**
     * Reads contents of 'outFile' {@link java.io.File} setting those contents
     * via {@link org.ndexbio.communitydetection.rest.model.CommunityDetectionResult#setResult(com.fasterxml.jackson.databind.JsonNode)}
     * This implementation is a bit naive it will first try to use Jackson's {@link com.fasterxml.jackson.databind.ObjectMapper#readTree(java.io.File)}
     * to parse the 'outFile' {@link java.io.File} and if that tosses an exception the code will
     * assume its raw text data and store the result as a {@link com.fasterxml.jackson.databind.node.TextNode}
     * @param cdr The object to update with results from 'outFile' {@link java.io.File}
     * @param outFile {@link java.io.File} to get data from
     * @throws Exception 
     */
    protected void updateCommunityDetectionResultWithFileContents(CommunityDetectionResult cdr, File outFile) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (outFile.isFile() == false){
            _logger.error(outFile.getAbsolutePath() + " does not exist or is not a file");
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            cdr.setResult(mapper.readTree(outFile));
        } catch(JsonParseException jpe){
            _logger.debug("Received a json parsing error going to try to store result as string: ", jpe);
            try (BufferedReader br = new BufferedReader(new FileReader(outFile))){
                String line = br.readLine();
                while(line != null){
                    sb.append(line).append("\n");
                    line = br.readLine();
                }
                cdr.setResult(new TextNode(sb.toString()));
            }
        }
    }
    
    /**
     * Updates the 'cdr' {@link org.ndexbio.communitydetection.rest.model.CommunityDetectionResult} status,
     * message, and result fields
     * 
     * @param exitValue  exit code of command line process
     * @param stdOutFile the {@link .java.io.File} containing standard output from command line process
     * @param stdErrFile the {@link .java.io.File} containing standard error from command line process
     * @param cdr {@link org.ndexbio.communitydetection.rest.model.CommunityDetectionResult} to update and persist to the filesystem
     * @throws Exception If there was a parsing/writing problem
     */
    protected void updateCommunityDetectionResult(int exitValue, File stdOutFile, File stdErrFile, CommunityDetectionResult cdr) throws Exception {
        
        File outFile = stdOutFile;
        if (exitValue != 0){
                cdr.setStatus(CommunityDetectionResult.FAILED_STATUS);
                if (exitValue == 500){
                    cdr.setMessage("Runtime limit exceeded");
                } else {
                    cdr.setMessage("Received non zero exit code: " +
                            Integer.toString(exitValue) + " when running algorithm for task: " + cdr.getId());
                }
                outFile = stdErrFile;
                _logger.error(cdr.getMessage());
        } else {
            
                cdr.setStatus(CommunityDetectionResult.COMPLETE_STATUS);
        }
        updateCommunityDetectionResultWithFileContents(cdr, outFile);
    }
    
    /**
     * Writes the command run by the commandline process in {@link #call() } to the 
     * filesystem storing contents at {@link #getCommandRunFile() }
     */
    protected void writeCommandRunToFile(){
        File outFile = getCommandRunFile();
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))){
            bw.write(_runner.getLastCommand());
        } catch(IOException io){
            _logger.error("Error writing command run to: " + outFile.getAbsolutePath(), io);
        }
    }
    
    /**
     * Runs the command line process set via the constructor storing output, error, and
     * command run by this process to the file system. 
     * @return {@link org.ndexbio.communitydetection.rest.model.CommunityDetectionResult} with status, message, and result set
     * @throws Exception if there was a problem with IO.
     */
    @Override
    public CommunityDetectionResult call() throws Exception {
        
        File workDir = new File(_workDir);
        
        String mapDir = _workDir + ":" + _workDir + _mountOptions;
        _runner.setWorkingDirectory(_workDir);
        
        String inputFile = writeInputFile();
        
        File stdOutFile = getStandardOutFile();
        File stdErrFile = getStandardErrorFile();
        
        CommunityDetectionResult cdr = createCommunityDetectionResult();
        
        try {
            if (workDir.isDirectory() == false){
                throw new Exception(_workDir + " directory does not exist");
            }
            ArrayList<String> mCmd = new ArrayList<String>();
            mCmd.add(_dockerCmd);
            mCmd.add("run");
            mCmd.add("--rm");
            mCmd.add("-v");
            mCmd.add(mapDir);
            mCmd.add(_dockerImage);
            if (_customParameters != null){
                _logger.debug("Custom Parameters is not null adding to command line call");
                for (String key : _customParameters.keySet()){
                    mCmd.add(key);
                    
                    String val = _customParameters.get(key);
                    if (val != null && val.trim().isEmpty() == false){
                        mCmd.add(val);
                    }
                }
            } else {
                _logger.debug("Custom Parameters is null");
            }
            mCmd.add(inputFile);
            int  exitValue = _runner.runCommandLineProcess(_timeOut, _timeUnit,
                    stdOutFile, stdErrFile, mCmd.toArray(new String[0]));
            writeCommandRunToFile();
            updateCommunityDetectionResult(exitValue, stdOutFile, stdErrFile, cdr);
            
        } catch(Exception ex){
            cdr.setStatus(CommunityDetectionResult.FAILED_STATUS);
            cdr.setMessage("Received error trying to run detection: " + ex.getMessage());
            _logger.error("Received error trying to run algorithm for task in " + _workDir, ex);
        }
        cdr.setProgress(100);
        cdr.setWallTime(System.currentTimeMillis() - cdr.getStartTime());
        return cdr;          
    }
    
}
