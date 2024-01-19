package org.ndexbio.communitydetection.rest.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.ndexbio.communitydetection.rest.engine.util.CommunityDetectionRequestValidator;
import org.ndexbio.communitydetection.rest.engine.util.CommunityDetectionRequestValidatorImpl;
import org.ndexbio.communitydetection.rest.model.AlgorithmCustomParameter;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithm;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithms;
import org.ndexbio.communitydetection.rest.model.CustomParameter;
import org.ndexbio.communitydetection.rest.model.ServiceAlgorithm;
import org.ndexbio.communitydetection.rest.model.ServiceMetaData;
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
	private ServiceMetaData _metaData;
    
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
		_metaData = getMetaData(config.getName(), config.getDescription(),
				config.getInputDataFormat(), config.getOutputDataFormat());
       
    }
	
	private ServiceMetaData getMetaData(final String name, final String description,
			final String inputDataFormat, final String outputDataFormat){
		ServiceMetaData metaData = new ServiceMetaData();
		ArrayList<ServiceAlgorithm> algos = new ArrayList<>();
		if (_algorithms != null && _algorithms.getAlgorithms() != null){
			for (String key : _algorithms.getAlgorithms().keySet()){
				CommunityDetectionAlgorithm cda = _algorithms.getAlgorithms().get(key);
				if (cda.getInputDataFormat() != null && cda.getInputDataFormat().equalsIgnoreCase("cx2")){
					algos.add(getServiceAlgorithm(cda));
				}
			}
		}
		metaData.setAlgorithms(algos);
		metaData.setName(name);
		metaData.setDescription(description);
		metaData.setInputDataFormat(inputDataFormat);
		metaData.setOutputDataFormat(outputDataFormat);
		return metaData;
	}
	
	private ServiceAlgorithm getServiceAlgorithm(CommunityDetectionAlgorithm cda){
		ServiceAlgorithm sa = new ServiceAlgorithm();
		sa.setDescription(cda.getDescription());
		sa.setDisplayName(cda.getDisplayName());
		sa.setVersion(cda.getVersion());
		sa.setName(cda.getName());
		HashSet<AlgorithmCustomParameter> parameters = new HashSet<>();
		if (cda.getCustomParameterMap() != null){
			for (CustomParameter cp : cda.getCustomParameterMap().values()){
				AlgorithmCustomParameter acp = new AlgorithmCustomParameter();
				acp.setDefaultValue(cp.getDefaultValue());
				acp.setDescription(cp.getDescription());
				acp.setDisplayName(cp.getDisplayName());
				acp.setMaxValue(cp.getMaxValue());
				acp.setMinValue(cp.getMinValue());
				acp.setType(cp.getType());
				acp.setValidationHelp(cp.getValidationHelp());
				acp.setValidationRegex(cp.getValidationRegex());
				acp.setValidationType(cp.getValidationType());
				parameters.add(acp);
			}
		}
		
		sa.setParameters(parameters);
		return sa;
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
                _dockerCmd, _algorithms, _metaData, _validator);
        return engine;
    }
}
