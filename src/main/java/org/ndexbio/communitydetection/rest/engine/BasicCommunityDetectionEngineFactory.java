package org.ndexbio.communitydetection.rest.engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.ndexbio.communitydetection.rest.engine.util.CommunityDetectionRequestValidator;
import org.ndexbio.communitydetection.rest.engine.util.CommunityDetectionRequestValidatorImpl;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithms;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;
import org.ndexbio.communitydetection.rest.services.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory to create {@link org.ndexbio.communitydetection.rest.engine.CommunityDetectionEngine} objects
 * 
 * @author churas
 */
public class BasicCommunityDetectionEngineFactory {
    
    static Logger _logger = LoggerFactory.getLogger(BasicCommunityDetectionEngineFactory.class);

    private int _numWorkers;
    private String _taskDir;
    private String _dockerCmd;
    private CommunityDetectionAlgorithms _algorithms;
    private CommunityDetectionRequestValidator _validator;
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param config Configuration containing number of workers, task directory, docker command,
     *               and algorithms.
     */
    public BasicCommunityDetectionEngineFactory(Configuration config){
        
        _numWorkers = config.getNumberWorkers();
        _taskDir = config.getTaskDirectory();
        _dockerCmd = config.getDockerCommand();
        _algorithms = config.getAlgorithms();
        _validator = new CommunityDetectionRequestValidatorImpl();
       
    }

    /**
     * Creates CommunityDetectionEngine with a fixed threadpool to process requests
     * @throws CommunityDetectionException if there is an error
     * @return {@link org.ndexbio.communitydetection.rest.engine.CommunityDetectionEngine} object 
     *         ready to service requests
     */
    public CommunityDetectionEngine getCommunityDetectionEngine() throws CommunityDetectionException {
        _logger.debug("Creating executor service with: " + Integer.toString(_numWorkers) + " workers");
        ExecutorService es = Executors.newFixedThreadPool(_numWorkers);
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(es, _taskDir,
                _dockerCmd, _algorithms, _validator);
        return engine;
    }
}
