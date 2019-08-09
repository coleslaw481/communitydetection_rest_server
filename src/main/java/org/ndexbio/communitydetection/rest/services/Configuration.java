package org.ndexbio.communitydetection.rest.services;
import com.fasterxml.jackson.core.JsonProcessingException;
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

/**
 * Contains configuration for Enrichment. The configuration
 * is extracted by looking for a file under the environment
 * variable NDEX_ENRICH_CONFIG and if that fails defaults are
 * used
 * @author churas
 */
public class Configuration {
    
    public static final String APPLICATION_PATH = "/communitydetection";
    public static final String V_ONE_PATH = "/v1";
    public static final String NDEX_ENRICH_CONFIG = "NDEX_ENRICH_CONFIG";
    
    public static final String DATABASE_DIR = "enrichment.database.dir";
    public static final String TASK_DIR = "enrichment.task.dir";
    public static final String HOST_URL = "enrichment.host.url";
    
    public static final String NDEX_USER = "ndex.user";
    public static final String NDEX_PASS = "ndex.password";
    public static final String NDEX_SERVER = "ndex.server";
    public static final String NDEX_USERAGENT = "ndex.useragent";
    
    
    public static final String DATABASE_RESULTS_JSON_FILE = "databaseresults.json";
    
    private static Configuration INSTANCE;
    private static final Logger _logger = LoggerFactory.getLogger(Configuration.class);
    private static String _alternateConfigurationFile;
    private static CommunityDetectionEngine _enrichmentEngine;
    private static String _enrichDatabaseDir;
    private static String _enrichTaskDir;
    private static String _enrichHostURL;
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
        
        _enrichDatabaseDir = props.getProperty(Configuration.DATABASE_DIR, "/tmp");
        _enrichTaskDir = props.getProperty(Configuration.TASK_DIR, "/tmp");
        _enrichHostURL = props.getProperty(Configuration.HOST_URL, "");
        if (_enrichHostURL.trim().isEmpty()){
            _enrichHostURL = "";
        } else if (!_enrichHostURL.endsWith("/")){
            _enrichHostURL =_enrichHostURL + "/";
        }
    }
        
    protected void setEnrichmentEngine(CommunityDetectionEngine ee){
        _enrichmentEngine = ee;
    }
    public CommunityDetectionEngine getEnrichmentEngine(){
        return _enrichmentEngine;
    }

    /**
     * Gets alternate URL prefix for the host running this service.
     * @return String containing alternate URL ending with / or empty
     *         string if not is set
     */
    public String getHostURL(){
        return _enrichHostURL;
    }
    
    /**
     * Gets directory where enrichment database is stored on the file system
     * @return 
     */
    public String getEnrichmentDatabaseDirectory(){
        return _enrichDatabaseDir;
    }
    
    /**
     * Gets directory where enrichment task results should be stored
     * @return 
     */
    public String getEnrichmentTaskDirectory(){
        return _enrichTaskDir;
    }

    public File getDatabaseResultsFile(){
        
        return new File(getEnrichmentDatabaseDirectory() + File.separator +
                              Configuration.DATABASE_RESULTS_JSON_FILE);
    }
    /**
     * 
     * @return 
     */
    public InternalDatabaseResults getNDExDatabases(){
        ObjectMapper mapper = new ObjectMapper();
        File dbres = getDatabaseResultsFile();
        try {
            return mapper.readValue(dbres, InternalDatabaseResults.class);
        }
        catch(IOException io){
            _logger.error("caught io exception trying to load " + dbres.getAbsolutePath(), io);
        }
        return null;
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
                        configPath = System.getenv(Configuration.NDEX_ENRICH_CONFIG);
                    } catch(SecurityException se){
                        _logger.error("Caught security exception ", se);
                    }
                }
                if (configPath == null){
                    InitialContext ic = new InitialContext();
                    configPath = (String) ic.lookup("java:comp/env/" + Configuration.NDEX_ENRICH_CONFIG); 

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
