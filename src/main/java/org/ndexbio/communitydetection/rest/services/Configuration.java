package org.ndexbio.communitydetection.rest.services;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ndexbio.communitydetection.rest.engine.CommunityDetectionEngine;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithms;

/**
 * Contains configuration for Enrichment. The configuration
 is extracted by looking for a file under the environment
 variable COMMUNITY_DETECTION_CONFIG and if that fails defaults are
 used
 * @author churas
 */
public class Configuration {
    
    //public static final String APPLICATION_PATH = "/communitydetection";
    public static final String V_ONE_PATH = "/v1";
    public static final String COMMUNITY_DETECTION_CONFIG = "COMMUNITY_DETECTION_CONFIG";
    
    public static final String TASK_DIR = "communitydetection.task.dir";
    public static final String HOST_URL = "communitydetection.host.url";    
    public static final String NUM_WORKERS = "communitydetection.number.workers";
    public static final String DOCKER_CMD = "communitydetection.docker.cmd";
    public static final String ALGORITHM_MAP = "communitydetection.algorithm.map";
    public static final String ALGORITHM_TIMEOUT = "communitydetection.algorithm.timeout";
    public static final String MOUNT_OPTIONS = "communitydetection.mount.options";
    public static final String SWAGGER_TITLE = "swagger.title";
    public static final String SWAGGER_DESC = "swagger.description";
    
    public static final String RUNSERVER_CONTEXTPATH = "runserver.contextpath";
    public static final String RUNSERVER_APP_PATH = "runserver.applicationpath";
    
    private static Configuration INSTANCE;
    private static final Logger _logger = LoggerFactory.getLogger(Configuration.class);
    private static String _alternateConfigurationFile;
    private static CommunityDetectionEngine _communityEngine;
    private static String _taskDir;
    private static String _hostURL;
    private static String _dockerCmd;
    private static int _numWorkers;
    private static CommunityDetectionAlgorithms _algorithms;
    private static long _timeOut;
    private String _mountOptions;
    private String _swaggerTitle;
    private String _swaggerDescription;
    private String _contextPath;
    private String _applicationPath;
    
    /**
     * Constructor that attempts to get configuration from properties file
     * specified via configPath
     */
    private Configuration(final String configPath) throws CommunityDetectionException
    {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configPath));
        }
        catch(FileNotFoundException fne){
            _logger.error("No configuration found at " + configPath, fne);
            throw new CommunityDetectionException("FileNotFound Exception when attempting to load " 
                    + configPath + " : " +
                    fne.getMessage());
        }
        catch(IOException io){
            _logger.error("Unable to read configuration " + configPath, io);
            throw new CommunityDetectionException("IOException when trying to read configuration file " + configPath +
                     " : " + io);
        }
        
        _taskDir = props.getProperty(Configuration.TASK_DIR, "/tmp");
        _numWorkers = Integer.parseInt(props.getProperty(Configuration.NUM_WORKERS, "1"));
        _hostURL = props.getProperty(Configuration.HOST_URL, "");
        _dockerCmd = props.getProperty(Configuration.DOCKER_CMD, "docker");
        _algorithms = getAlgorithms(props.getProperty(Configuration.ALGORITHM_MAP, null));
        _timeOut = Long.parseLong(props.getProperty(Configuration.ALGORITHM_TIMEOUT, "180"));
        _mountOptions = props.getProperty(Configuration.MOUNT_OPTIONS, ":ro");
        _swaggerTitle = props.getProperty(Configuration.SWAGGER_TITLE, null);
        _swaggerDescription = props.getProperty(Configuration.SWAGGER_DESC, null);
        _contextPath = props.getProperty(Configuration.RUNSERVER_CONTEXTPATH, "/cd");
        _applicationPath = props.getProperty(Configuration.RUNSERVER_APP_PATH, "/communitydetection");
        if (_hostURL.trim().isEmpty()){
            _hostURL = "";
        } else if (!_hostURL.endsWith("/")){
            _hostURL =_hostURL + "/";
        }
    }
    
    protected CommunityDetectionAlgorithms getAlgorithms(final String algoPath){
        if (algoPath == null){
            _logger.error("Path to algorithms json file is null");
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            File algoFile = new File(algoPath);
            if (algoFile.isFile() == false){
                _logger.error(algoFile.getAbsolutePath() + " is not a file");
                return null;
            }
            return mapper.readValue(algoFile, CommunityDetectionAlgorithms.class);
        }
        catch(IOException io){
              _logger.error("Error parsing json: " + algoPath + " : " + io.getMessage());
        }
        
        return null;
    }
        
    protected void setCommunityDetectionEngine(CommunityDetectionEngine ee){
        _communityEngine = ee;
    }
    public CommunityDetectionEngine getCommunityDetectionEngine(){
        return _communityEngine;
    }

    /**
     * Gets alternate URL prefix for the host running this service.
     * @return String containing alternate URL ending with / or empty
     *         string if not is set
     */
    public String getHostURL(){
        return _hostURL;
    }
    
    /**
     * Gets directory where enrichment task results should be stored
     * @return 
     */
    public String getTaskDirectory(){
        return _taskDir;
    }
    
    public int getNumberWorkers(){
        return _numWorkers;
    }
    
    public long getAlgorithmTimeOut(){
        return _timeOut;
    }
    
    public String getDockerCommand(){
        return _dockerCmd;
    }
    
    public CommunityDetectionAlgorithms getAlgorithms(){
        return _algorithms;
    }
    
    public String getMountOptions(){
        return _mountOptions;
    }
    
    public String getSwaggerTitle(){
        return _swaggerTitle;
    }
    
    public String getSwaggerDescription(){
        return _swaggerDescription;
    }
    
    public String getRunServerContextPath(){
        return _contextPath;
    }
    
    public String getRunServerApplicationPath(){
        return _applicationPath;
    }
    
    
    public String getSwaggerServer(){
        return getRunServerContextPath() + getRunServerApplicationPath();
    }
    
    /**
     * Gets singleton instance of configuration
     * @return {@link org.ndexbio.communitydetection.rest.services.Configuration} object with configuration loaded
     * @throws EnrichmentException if there was a problem reading the configuration
     */
    public static Configuration getInstance() throws CommunityDetectionException
    {
    	if (INSTANCE == null)  { 
            
            try {
                String configPath = null;
                if (_alternateConfigurationFile != null){
                    configPath = _alternateConfigurationFile;
                    _logger.info("Alternate configuration path specified: " + configPath);
                } else {
                    try {
                        configPath = System.getenv(Configuration.COMMUNITY_DETECTION_CONFIG);
                    } catch(SecurityException se){
                        _logger.error("Caught security exception ", se);
                    }
                }
                if (configPath == null){
                    InitialContext ic = new InitialContext();
                    configPath = (String) ic.lookup("java:comp/env/" + Configuration.COMMUNITY_DETECTION_CONFIG); 

                }
                INSTANCE = new Configuration(configPath);
            } catch (NamingException ex) {
                _logger.error("Error loading configuration", ex);
                throw new CommunityDetectionException("NamingException encountered. Error loading configuration: " 
                         + ex.getMessage());
            }
    	} 
        return INSTANCE;
    }
    
    /**
     * Reloads configuration
     * @return {@link org.ndexbio.communitydetection.rest.services.Configuration} object
     * @throws EnrichmentException if there was a problem reading the configuration
     */
    public static Configuration reloadConfiguration() throws CommunityDetectionException  {
        INSTANCE = null;
        return getInstance();
    }
    
    /**
     * Lets caller set an alternate path to configuration. Added so the command
     * line application can set path to configuration and it makes testing easier
     * This also sets the internal instance object to {@code null} so subsequent
     * calls to {@link #getInstance() } will load a new instance with this configuration
     * @param configFilePath - Path to configuration file
     */
    public static void  setAlternateConfigurationFile(final String configFilePath) {
    	_alternateConfigurationFile = configFilePath;
        INSTANCE = null;
    }
}
