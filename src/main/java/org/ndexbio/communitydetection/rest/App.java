package org.ndexbio.communitydetection.rest;


import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.RolloverFileOutputStream;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.FilterDispatcher;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithm;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithms;
import org.ndexbio.communitydetection.rest.model.CustomParameter;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.ndexbio.communitydetection.rest.services.Configuration;
import org.ndexbio.communitydetection.rest.services.CommunityDetectionHttpServletDispatcher;
import org.ndexbio.communitydetection.rest.services.OpenApiHttpServletDispatcher;


/**
 *
 * @author churas
 */
public class App {
    
    static Logger _logger = LoggerFactory.getLogger(App.class);

    public static final String DESCRIPTION = "\nNDEx Community Detection REST service\n\n"
            + "For usage information visit:  https://github.com/ndexbio/communitydetection-rest-server\n\n";
    
    /**
     * Default name for community detection algorithms json file
     */
    public static final String CD_ALGORITHMS_FILE = "communitydetectionalgorithms.json";
    
    /**
     * Sets logging level valid values DEBUG INFO WARN ALL ERROR
     */
    public static final String RUNSERVER_LOGLEVEL = "runserver.log.level";

    /**
     * Sets log directory for embedded Jetty
     */
    public static final String RUNSERVER_LOGDIR = "runserver.log.dir";
    
    /**
     * Sets port for embedded Jetty
     */
    public static final String RUNSERVER_PORT = "runserver.port";
        
    /**
     * Sets context path for embedded Jetty
     */
    
    public static final String RUNSERVER_DOSFILTER_MAX_REQS = "runserver.dosfilter.maxrequestspersec";
    
    public static final String RUNSERVER_DOSFILTER_DELAY = "runserver.dosfilter.delayms";
    
    public static final String MODE = "mode";
    public static final String CONF = "conf";    
    public static final String EXAMPLE_CONF_MODE = "exampleconf";
    public static final String EXAMPLE_ALGO_MODE = "examplealgo";
    public static final String RUNSERVER_MODE = "runserver";
    
    public static final String SUPPORTED_MODES = EXAMPLE_CONF_MODE + ", " +
                                                 EXAMPLE_ALGO_MODE +
                                                    ", " + RUNSERVER_MODE;
    
    public static void main(String[] args){

        final List<String> helpArgs = Arrays.asList("h", "help", "?");
        try {
            OptionParser parser = new OptionParser() {

                {
                    accepts(MODE, "Mode to run. Supported modes: " + SUPPORTED_MODES).withRequiredArg().ofType(String.class).required();
                    accepts(CONF, "Configuration file")
                            .withRequiredArg().ofType(String.class);
                    acceptsAll(helpArgs, "Show Help").forHelp();
                }
            };
            
            OptionSet optionSet = null;
            try {
                optionSet = parser.parse(args);
            } catch (OptionException oe) {
                System.err.println("\nThere was an error parsing arguments: "
                        + oe.getMessage() + "\n\n");
                parser.printHelpOn(System.err);
                System.exit(1);
            }

            //help check
            for (String helpArgName : helpArgs) {
                if (optionSet.has(helpArgName)) {
                    System.out.println(DESCRIPTION);
                    parser.printHelpOn(System.out);
                    System.exit(2);
                }
            }
            
            String mode = optionSet.valueOf(MODE).toString();

            if (mode.equals(EXAMPLE_CONF_MODE)){
                System.out.println(generateExampleConfiguration());
                System.out.flush();
                return;
            }
            
            if (mode.equals(EXAMPLE_ALGO_MODE)){
                System.out.println(generateExampleCommunityDetectionAlgorithms());
                System.out.flush();
                return;
            }
      
            if (mode.equals(RUNSERVER_MODE)){
                Configuration.setAlternateConfigurationFile(optionSet.valueOf(CONF).toString());
                Properties props = getPropertiesFromConf(optionSet.valueOf(CONF).toString());
                ch.qos.logback.classic.Logger rootLog = 
        		(ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                rootLog.setLevel(Level.toLevel(props.getProperty(App.RUNSERVER_LOGLEVEL, "INFO")));

                String logDir = props.getProperty(App.RUNSERVER_LOGDIR, ".");
                RolloverFileOutputStream os = new RolloverFileOutputStream(logDir 
                        + File.separator + "communitydetection_yyyy_mm_dd.log", true);
		
		
                final int port = Integer.valueOf(props.getProperty(App.RUNSERVER_PORT, "8081"));
                String applicationPath = App.getApplicationPath(props);
                System.out.println("\nSpinning up server. For status visit: \nhttp://localhost:" 
                        + Integer.toString(port) + props.getProperty(Configuration.RUNSERVER_CONTEXTPATH) 
                        + applicationPath + Configuration.V_ONE_PATH + "/status\n");
                System.out.println("Swagger documentation: " + "http://localhost:" 
                        + Integer.toString(port) + props.getProperty(Configuration.RUNSERVER_CONTEXTPATH) + "\n");
                System.out.flush();
                
                //We are creating a print stream based on our RolloverFileOutputStream
		PrintStream logStream = new PrintStream(os);

                //We are redirecting system out and system error to our print stream.
		System.setOut(logStream);
		System.setErr(logStream);

                final Server server = new Server(port);

                final ServletContextHandler webappContext = new ServletContextHandler(server,
                        Configuration.getInstance().getRunServerContextPath());
                
                
                HashMap<String, String> initMap = new HashMap<>();
                initMap.put("resteasy.servlet.mapping.prefix",
                             applicationPath + "/");
                
                initMap.put("javax.ws.rs.Application",
                        "org.ndexbio.communitydetection.rest.CommunityDetectionApplication");
                final ServletHolder restEasyServlet = new ServletHolder(
                     new CommunityDetectionHttpServletDispatcher());
                
                restEasyServlet.setInitOrder(1);
                restEasyServlet.setInitParameters(initMap);
                webappContext.addServlet(restEasyServlet,
                                          applicationPath + "/*");
                webappContext.addFilter(CorsFilter.class,
                                        applicationPath + "/*", null);
                webappContext.addFilter(FilterDispatcher.class, "/*", null);
                
                
                final ServletHolder openApiServlet = new ServletHolder(new OpenApiHttpServletDispatcher());
                HashMap<String, String> openApiInitMap = new HashMap<>();
                initMap.put("resteasy.servlet.mapping.prefix",
                             "/openapi/");
                openApiInitMap.put("openApi.configuration.filterClass",
                        "org.ndexbio.communitydetection.rest.SwaggerFilter");
                openApiInitMap.put("openApi.configuration.prettyPrint", "true");
                openApiInitMap.put("javax.ws.rs.Application",
                        "org.ndexbio.communitydetection.rest.OpenApiApplication");
                openApiInitMap.put("openApi.configuration.resourceClasses",
                        getClassesToParseByOpenApi());
                openApiInitMap.put("openApi.configuration.resourcePackages",
                        "org.ndexbio.communitydetection.rest.model");
                openApiServlet.setInitOrder(2);
                openApiServlet.setInitParameters(openApiInitMap);
                webappContext.addServlet(openApiServlet, "/openapi.json");

                String resourceBasePath = App.class.getResource("/webapp").toExternalForm();
                webappContext.setWelcomeFiles(new String[] { "index.html" });
                webappContext.setResourceBase(resourceBasePath);
                webappContext.addServlet(new ServletHolder(new DefaultServlet()), "/*");
                
                ContextHandlerCollection contexts = new ContextHandlerCollection();
                contexts.setHandlers(new Handler[] { webappContext });
 
                server.setHandler(contexts);
                   
                server.start();	    
                System.out.println("Server started on: " + server.getURI().toString());
                server.join();
                return;
            }
            System.err.println("Invalid --mode: " + mode + " mode must be one of the "
                    + "following: " + SUPPORTED_MODES);
            System.exit(3);
      
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }
    
    /**
     * Gets Comma delimited list of classes that should be parsed by OpenAPI to
     * generate Swagger documentation. If a diffusion algorithm was set in config,
     * then that class will also be added.
     * 
     * @return Comma delimited list of classes as a String
     */
    public static String getClassesToParseByOpenApi(){
        StringBuilder sb = new StringBuilder();
        sb.append("org.ndexbio.communitydetection.rest.services.CommunityDetection,");
        sb.append("org.ndexbio.communitydetection.rest.services.Status");
        try {
            if (Configuration.getInstance().getDiffusionAlgorithm() != null){
                sb.append(",org.ndexbio.communitydetection.rest.services.Diffusion");
            }
        } catch(CommunityDetectionException cde){
            _logger.error("Unable to check if diffusion algorithm is in config", cde);
        }
        return sb.toString();
    }
    
    /**
     * Gets {@link org.ndexbio.communitydetection.rest.services.Configuration#RUNSERVER_APP_PATH} 
     * from properties passed in
     * @param props Properties to examine
     * @return Application path
     */
    public static String getApplicationPath(Properties props){
        return props.getProperty(Configuration.RUNSERVER_APP_PATH, "/communitydetection");
    }
    
    /**
     * Loads properties from configuration file specified by {@code path}
     * @param path Path to configuration file
     * @return Properties found in configuration file passed in
     * @throws IOException thrown by {@link java.util.Properties#load(java.io.InputStream)}
     * @throws FileNotFoundException thrown by {@link java.util.Properties#load(java.io.InputStream)}
     */
    public static Properties getPropertiesFromConf(final String path) throws IOException, FileNotFoundException {
        Properties props = new Properties();
        props.load(new FileInputStream(path));
        return props;
    }
    
    /**
     * Generates an example community detection algorithms json string
     * with actual docker images
     * @return json string
     * @throws Exception If there is an error
     */
    public static String generateExampleCommunityDetectionAlgorithms() throws Exception {
        
         LinkedHashMap<String, CommunityDetectionAlgorithm> algoSet = new LinkedHashMap<>();
        //gprofiler term mapper
        CommunityDetectionAlgorithm cda = new CommunityDetectionAlgorithm();
        cda.setName("gprofilersingletermv2");
        algoSet.put(cda.getName(), cda);
        cda.setDescription("Uses gprofiler to find best term below pvalue cut off"
                + "using a list of genes as input");
        cda.setDockerImage("coleslawndex/gprofilersingletermv2");
        cda.setInputDataFormat("GENELIST");
        cda.setOutputDataFormat("MAPPEDTERMJSON");
        cda.setVersion("1.0.0");
        CustomParameter cp = new CustomParameter();
        cp.setDescription("Maximum pvalue to allow for results");
        cp.setName("--maxpval");
        cp.setType("value");
        cp.setDefaultValue("0.00001");
        cp.setDisplayName("Maximum Pvalue");
        cp.setValidationHelp("Must be a number");
        cp.setValidationType("number");
        HashSet<CustomParameter> cpSet = new HashSet<>();
        cpSet.add(cp);
        cda.setCustomParameters(cpSet);
        
        //louvain
        CommunityDetectionAlgorithm cdb = new CommunityDetectionAlgorithm();
        cdb.setName("louvain");
        algoSet.put(cdb.getName(), cdb );
        cdb.setDescription("Runs louvain community detection algorithm");
        cdb.setDockerImage("ecdymore/slouvaintest");
        cdb.setInputDataFormat("EDGELIST");
        cdb.setOutputDataFormat("COMMUNITYDETECTRESULT");
        cdb.setVersion("2.0.0");
        
        cp = new CustomParameter();
        cp.setName("--directed");
        cp.setDescription("If set, generate directed graph");
        cp.setDisplayName("Generate directed graph");
        cp.setType("flag");
        cpSet = new HashSet<>();
        cpSet.add(cp);
        
        cp = new CustomParameter();
        cp.setName("--configmodel");
        cp.setDescription("Configuration model which must be one of following:"
                + ": RB, RBER, CPM, Suprise, Significance, Default");
        cp.setDisplayName("Configuration Model");
        cp.setType("value");
        cp.setDefaultValue("Default");
        cp.setValidationType("string");
        cp.setValidationHelp("Must be one of following: RB, RBER, CPM, Suprise, Significance, Default");
        cp.setValidationRegex("RB|RBER|CPM|Suprise|Significance|Default");
        cpSet.add(cp);
        
        cdb.setCustomParameters(cpSet);
        
        //infomap
        CommunityDetectionAlgorithm cdc = new CommunityDetectionAlgorithm();
        cdc.setName("infomap");
        algoSet.put(cdc.getName(), cdc);
        cdc.setDescription("Runs infomap community detection algorithm");
        cdc.setDockerImage("ecdymore/sinfomaptest");
        cdc.setInputDataFormat("EDGELIST");
        cdc.setOutputDataFormat("COMMUNITYDETECTRESULT");
        cdc.setVersion("2.0.0");
        
        cp = new CustomParameter();
        cp.setName("--directed");
        cp.setDescription("If set, infomap assumes directed links");
        cp.setDisplayName("Assume Directed Links");
        cp.setType("flag");
        cpSet = new HashSet<>();
        cpSet.add(cp);
        
        cp = new CustomParameter();
        cp.setName("--enableoverlapping");
        cp.setDescription("If set, Let nodes be part of different and "
                + "overlapping modules. Applies to ordinary networks by "
                + "first representing the memoryless dynamics with memory "
                + "nodes.");
        cp.setDisplayName("Enable Overlapping");
        cp.setType("flag");
        cpSet.add(cp);
        
        cp = new CustomParameter();
        cp.setName("--markovtime");
        cp.setDescription("Scale link flow with this value to change the cost "
                + "of moving between modules. Higher for less modules");
        cp.setDisplayName("Markov time");
        cp.setType("value");
        cp.setDefaultValue("0.75");
        cp.setValidationType("number");
        cp.setValidationHelp("Should be a number");
        cpSet.add(cp);
        cdc.setCustomParameters(cpSet);
        
        
        //clixo
        CommunityDetectionAlgorithm cde = new CommunityDetectionAlgorithm();
        cde.setName("clixo");
        algoSet.put(cde.getName(), cde);
        cde.setDescription("Runs clixo community detection algorithm");
        cde.setDockerImage("coleslawndex/clixo:1.0");
        cde.setInputDataFormat("EDGELIST");
        cde.setOutputDataFormat("COMMUNITYDETECTRESULT");
        cde.setVersion("2.0.0");
        
        cp = new CustomParameter();
        cp.setName("--alpha");
        cp.setDescription("Threshold between clusters");
        cp.setDisplayName("Alpha");
        cp.setType("value");
        cp.setDefaultValue("0.1");
        cp.setValidationType("number");
        
        cpSet = new HashSet<>();
        cpSet.add(cp);
        
        cp = new CustomParameter();
        cp.setName("--beta");
        cp.setDescription("Merge similarity for overlapping clusters");
        cp.setDisplayName("Beta");
        cp.setType("value");
        cp.setDefaultValue("0.5");
        cp.setValidationType("number");
        cpSet.add(cp);
        cde.setCustomParameters(cpSet);
        
        CommunityDetectionAlgorithms algos = new CommunityDetectionAlgorithms();
        algos.setAlgorithms(algoSet);
        ObjectMapper mappy = new ObjectMapper();
        return mappy.writerWithDefaultPrettyPrinter().writeValueAsString(algos);
    }
   
    /**
     * Generates example Configuration file writing to standard out
     * @throws Exception if there is an error
     * @return example configuration
     */
    public static String generateExampleConfiguration() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# Example configuration file for Community Detection service\n\n");
        
        sb.append("# Sets Community Detection task directory where results from queries are stored\n");
        sb.append(Configuration.TASK_DIR + " = /tmp/tasks\n\n");
        
        sb.append("# Sets number of workers to use to run tasks\n");
        sb.append(Configuration.NUM_WORKERS + " = 1\n\n");
        
        sb.append("# Docker command to run\n");
        sb.append(Configuration.DOCKER_CMD + " = docker\n\n");
        
        sb.append("# Algorithm/ docker command timeout in seconds. Anything taking longer will be killed\n");
        sb.append(Configuration.ALGORITHM_TIMEOUT + " = 180\n\n");
        
        sb.append("# Path to file containing json of algorithms\n");
        sb.append(Configuration.ALGORITHM_MAP + " = " + CD_ALGORITHMS_FILE + "\n\n");
        
        sb.append("# Name of Diffusion algorithm to serve on legacy POST endpoint\n");
        sb.append("# NOTE: Should be set to name of algorithm\n");
        sb.append("#       in algorithms json file.\n");
        sb.append("# (Only set this to set up a diffusion service, can be commented out)\n");
        sb.append("# " + Configuration.DIFFUSION_ALGO + " = networkheatdiffusion\n\n");
        
        sb.append("# Delay in milliseconds to wait while checking for completion of Diffusion\n");
        sb.append("# (Only relevant if setting up diffusion service, can be commented out)\n");
        sb.append("# " + Configuration.DIFFUSION_POLLDELAY + " = 100\n\n");
        
        sb.append("# Mount options, if unset :ro is used (podman may require :ro,z)\n");
        sb.append(Configuration.MOUNT_OPTIONS + " = :ro\n\n");
        
        sb.append("# Sets HOST URL prefix (value is prefixed to Location header when query is invoked. Can be commented out)\n");
        sb.append("# " + Configuration.HOST_URL + " = http://ndexbio.org\n\n");
        
        sb.append("# If set, overrides title shown in Swagger and openapi.json\n");
        sb.append("# " + Configuration.SWAGGER_TITLE + " = my service\n\n");
        
        sb.append("# If set, overrides description shown in Swagger and openapi.json\n");
        sb.append("# " + Configuration.SWAGGER_DESC + " = description of my service\n\n");
        
        sb.append("# Sets directory where log files will be written for Jetty web server\n");
        sb.append(App.RUNSERVER_LOGDIR + " = /tmp/logs\n\n");
        
        sb.append("# Sets port Jetty web service will be run under\n");
        sb.append(App.RUNSERVER_PORT + " = 8081\n\n");
        
        sb.append("# Sets Jetty Context Path for Community Detection\n");
        sb.append(Configuration.RUNSERVER_CONTEXTPATH + " = /cd\n\n");
        
        sb.append("# Valid log levels DEBUG INFO WARN ERROR ALL\n");
        sb.append(App.RUNSERVER_LOGLEVEL + " = INFO\n");

        return sb.toString();
    }
}
