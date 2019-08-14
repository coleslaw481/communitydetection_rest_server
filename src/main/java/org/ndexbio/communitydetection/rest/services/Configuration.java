package org.ndexbio.communitydetection.rest.services;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ndexbio.communitydetection.rest.engine.CommunityDetectionEngine;

/**
 * Contains configuration for Enrichment. The configuration
 is extracted by looking for a file under the environment
 variable COMMUNITY_DETECTION_CONFIG and if that fails defaults are
 used
 * @author churas
 */
public class Configuration {
    
    public static final String APPLICATION_PATH = "/communitydetection";
    public static final String V_ONE_PATH = "/v1";
    public static final String COMMUNITY_DETECTION_CONFIG = "COMMUNITY_DETECTION_CONFIG";
    
    public static final String TASK_DIR = "communitydetection.task.dir";
    public static final String HOST_URL = "communitydetection.host.url";    
    public static final String NUM_WORKERS = "communitydetection.number.workers";
    public static final String DOCKER_CMD = "communitydetection.docker.cmd";
    public static final String ALGORITHM_MAP = "communitydetection.algorithm.map";
    
    private static Configuration INSTANCE;
    private static final Logger _logger = LoggerFactory.getLogger(Configuration.class);
    private static String _alternateConfigurationFile;
    private static CommunityDetectionEngine _communityEngine;
    private static String _taskDir;
    private static String _hostURL;
    private static String _dockerCmd;
    private static int _numWorkers;
    private static HashMap<String, String> _algoMap;
    
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
        _algoMap = parseAlgorithmMap(props.getProperty(Configuration.ALGORITHM_MAP, "{\"infomap\": \"coleslawndex/infomap\"}"));
        if (_hostURL.trim().isEmpty()){
            _hostURL = "";
        } else if (!_hostURL.endsWith("/")){
            _hostURL =_hostURL + "/";
        }
    }
    
    protected HashMap<String, String> parseAlgorithmMap(final String algoMapString){
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, String> hMap = new HashMap<String, String>();
        try {
            JsonNode actualObj = mapper.readTree(algoMapString);
            Iterator<Entry<String, JsonNode>> fields = actualObj.fields();
            _logger.info("Algorithm to docker image mapping: ");
            while(fields.hasNext()){
                Entry<String, JsonNode> jsonField = fields.next();
                _logger.info(jsonField.getKey() + " => " + jsonField.getValue().asText());
                hMap.put(jsonField.getKey(), jsonField.getValue().asText());
            }
            return hMap;
        }
        catch(IOException io){
              _logger.error("Error parsing json: " + algoMapString + " : " + io.getMessage());
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
    
    public String getDockerCommand(){
        return _dockerCmd;
    }
    
    public HashMap<String, String> getAlgorithmToDockerMap(){
        return _algoMap;
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
