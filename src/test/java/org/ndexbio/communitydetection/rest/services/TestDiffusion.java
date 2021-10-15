package org.ndexbio.communitydetection.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import javax.ws.rs.core.MediaType;
import org.easymock.Capture;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.communitydetection.rest.engine.CommunityDetectionEngine;
import org.ndexbio.communitydetection.rest.model.CXMateResult;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithm;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithms;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResult;


/**
 *
 * @author churas
 */
public class TestDiffusion {
	
	public static final String YOUMUSTUSEPOST = "{\"data\":\"\",\"errors\":[{\"type\":"
			+ "\"urn:cytoscape:communitydetection:unknown:405\",\"message\":\"youmustusethe"
			+ "POSTmethodwiththisendpoint\",\"link\":\"https://github.com/cytoscape/commun"
			+ "itydetection-rest-server\",\"status\":405}]}";

	@Rule
    public TemporaryFolder _folder = new TemporaryFolder();

	@After
	public void tearDown(){
		try {
			Configuration.getInstance().setCommunityDetectionEngine(null);
		} catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public Dispatcher getDispatcher(){
		Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
		dispatcher.getRegistry().addSingletonResource(new Diffusion());
		return dispatcher;
	}
	
	public static String writeConfigurationForDiffusion(final String tempDir){
		CommunityDetectionAlgorithms cdAlgos = new CommunityDetectionAlgorithms();
		CommunityDetectionAlgorithm someAlgo = new CommunityDetectionAlgorithm();
		someAlgo.setInputDataFormat("EDGELISTV2");
		someAlgo.setOutputDataFormat("COMMUNITYDETECTRESULTV2");
		someAlgo.setName("foo");
		someAlgo.setDisplayName("Foo");
		CommunityDetectionAlgorithm diffAlgo = new CommunityDetectionAlgorithm();
		diffAlgo.setInputDataFormat("CXMATE_INPUT");
		diffAlgo.setOutputDataFormat("CXMATE_OUTPUT");
		diffAlgo.setName("legacydiffusion");
		diffAlgo.setDisplayName("Legacy Diffusion");
		LinkedHashMap<String, CommunityDetectionAlgorithm> algoList = new LinkedHashMap<>();
		algoList.put(someAlgo.getName(), someAlgo);
		algoList.put(diffAlgo.getName(), diffAlgo);
		cdAlgos.setAlgorithms(algoList);
		ObjectMapper oMapper = new ObjectMapper();
		File algoConfig = new File(tempDir + File.separator + "algo.json");
		try {
			oMapper.writeValue(algoConfig, cdAlgos);
		} catch(Exception ex){
			ex.printStackTrace();
		}
		return algoConfig.getAbsolutePath();
	}
	
	
	@Test
	public void testLegacyDiffusionDelete() throws Exception {   
		Dispatcher dispatcher = getDispatcher();

		MockHttpRequest request = MockHttpRequest.delete(Configuration.LEGACY_DIFFUSION_PATH);

		MockHttpResponse response = new MockHttpResponse();            
		dispatcher.invoke(request, response);
		assertEquals(405, response.getStatus());
		assertEquals(TestDiffusion.YOUMUSTUSEPOST,
				response.getContentAsString().replaceAll("\\s+", ""));
	}
	
	@Test
	public void testLegacyDiffusionGet() throws Exception {   
		Dispatcher dispatcher = getDispatcher();

		MockHttpRequest request = MockHttpRequest.get(Configuration.LEGACY_DIFFUSION_PATH);

		MockHttpResponse response = new MockHttpResponse();            
		dispatcher.invoke(request, response);
		assertEquals(405, response.getStatus());
		assertEquals(TestDiffusion.YOUMUSTUSEPOST,
				response.getContentAsString().replaceAll("\\s+", ""));
	}
	
	@Test
	public void testLegacyDiffusionPut() throws Exception {   
		Dispatcher dispatcher = getDispatcher();

		MockHttpRequest request = MockHttpRequest.put(Configuration.LEGACY_DIFFUSION_PATH);

		MockHttpResponse response = new MockHttpResponse();            
		dispatcher.invoke(request, response);
		assertEquals(405, response.getStatus());
		assertEquals(TestDiffusion.YOUMUSTUSEPOST,
				response.getContentAsString().replaceAll("\\s+", ""));
	}
	
	@Test
    public void testLegacyDiffusionNoAlgorithmNoEngine() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = getDispatcher();
			
            MockHttpRequest request = MockHttpRequest.post(Configuration.LEGACY_DIFFUSION_PATH);
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);
            
            request.content(omappy.writeValueAsBytes(new TextNode("hi")));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setCommunityDetectionEngine(null);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CXMateResult cxRes = mapper.readValue(response.getOutput(),
                    CXMateResult.class);
            assertEquals("Error running diffusion : No diffusion algorithm found", cxRes.getErrors().get(0).getMessage());
        } finally {
            _folder.delete();
        }
    }
	
	@Test
    public void testLegacyDiffusionNoEngine() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
			fw.write(Configuration.ALGORITHM_MAP + " = "
					+ TestDiffusion.
							writeConfigurationForDiffusion(tempDir.getAbsolutePath()));
            fw.flush();
            fw.close();
            Dispatcher dispatcher = getDispatcher();
			
            MockHttpRequest request = MockHttpRequest.post(Configuration.LEGACY_DIFFUSION_PATH);
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);
            
            request.content(omappy.writeValueAsBytes(new TextNode("hi")));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setCommunityDetectionEngine(null);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CXMateResult cxRes = mapper.readValue(response.getOutput(),
                    CXMateResult.class);
            assertEquals("Error running diffusion : CommunityDetection Engine not loaded", cxRes.getErrors().get(0).getMessage());
        } finally {
            _folder.delete();
        }
    }
	
	@Test
    public void testLegacyDiffusionNoIdFromEngine() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
			fw.write(Configuration.ALGORITHM_MAP + " = "
					+ TestDiffusion.
							writeConfigurationForDiffusion(tempDir.getAbsolutePath()));
            fw.flush();
            fw.close();
            Dispatcher dispatcher = getDispatcher();
			
            MockHttpRequest request = MockHttpRequest.post(Configuration.LEGACY_DIFFUSION_PATH);
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);
            
            request.content(omappy.writeValueAsBytes(new TextNode("hi")));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
			CommunityDetectionEngine mockEngine = createMock(CommunityDetectionEngine.class);
			expect(mockEngine.request(notNull())).andReturn(null);
            replay(mockEngine);
			
            Configuration.getInstance().setCommunityDetectionEngine(mockEngine);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CXMateResult cxRes = mapper.readValue(response.getOutput(),
                    CXMateResult.class);
            assertEquals("Error running diffusion : No id returned from CommunityDetection Engine", cxRes.getErrors().get(0).getMessage());
			verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }
	
	@Test
    public void testLegacyDiffusionTaskFailed() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
			fw.write(Configuration.DIFFUSION_POLLDELAY + " = 0\n");
			fw.write(Configuration.ALGORITHM_MAP + " = "
					+ TestDiffusion.
							writeConfigurationForDiffusion(tempDir.getAbsolutePath()) + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = getDispatcher();
            MockHttpRequest request = MockHttpRequest.post(Configuration.LEGACY_DIFFUSION_PATH);
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);
            
            request.content(omappy.writeValueAsBytes(TextNode.valueOf("hi")));

			CommunityDetectionResult inCompleteTask = new CommunityDetectionResult();
			inCompleteTask.setProgress(99);
			CommunityDetectionResult completeTask = new CommunityDetectionResult();
			completeTask.setProgress(100);
			completeTask.setStatus(CommunityDetectionResult.FAILED_STATUS);
			completeTask.setResult(new TextNode("some error"));
            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
			CommunityDetectionEngine mockEngine = createMock(CommunityDetectionEngine.class);
			expect(mockEngine.request(notNull())).andReturn("12345");
			expect(mockEngine.getResult("12345")).andReturn(inCompleteTask).andReturn(completeTask);
            replay(mockEngine);
			
            Configuration.getInstance().setCommunityDetectionEngine(mockEngine);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CXMateResult cxRes = mapper.readValue(response.getOutput(),
                    CXMateResult.class);
            assertEquals("some error", cxRes.getErrors().get(0).getMessage());
			verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testLegacyDiffusionTaskSuccessInvalidInputCX() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
			fw.write(Configuration.DIFFUSION_POLLDELAY + " = 0\n");
			fw.write(Configuration.ALGORITHM_MAP + " = "
					+ TestDiffusion.
							writeConfigurationForDiffusion(tempDir.getAbsolutePath()) + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = getDispatcher();
            MockHttpRequest request = MockHttpRequest.post(Configuration.LEGACY_DIFFUSION_PATH);
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);
            
            request.content("{".getBytes());

			CommunityDetectionResult completeTask = new CommunityDetectionResult();
			completeTask.setProgress(100);
			completeTask.setStatus(CommunityDetectionResult.COMPLETE_STATUS);
			CXMateResult cxMateRes = new CXMateResult();
			cxMateRes.setData(new TextNode("success"));
			completeTask.setResult(omappy.readTree(cxMateRes.asJson()));
            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
	    CommunityDetectionEngine mockEngine = createMock(CommunityDetectionEngine.class);			

            Configuration.getInstance().setCommunityDetectionEngine(mockEngine);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CXMateResult cxRes = mapper.readValue(response.getOutput(),
                    CXMateResult.class);
            assertEquals(1, cxRes.getErrors().size());
            assertTrue(cxRes.getErrors().get(0).getMessage().contains("Error parsing input data"));
            
        } finally {
            _folder.delete();
        }
    }
	
    @Test
    public void testLegacyDiffusionTaskSuccessNoQueryArgs() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
			fw.write(Configuration.DIFFUSION_POLLDELAY + " = 0\n");
			fw.write(Configuration.ALGORITHM_MAP + " = "
					+ TestDiffusion.
							writeConfigurationForDiffusion(tempDir.getAbsolutePath()) + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = getDispatcher();
            MockHttpRequest request = MockHttpRequest.post(Configuration.LEGACY_DIFFUSION_PATH);
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);
            
            request.content(omappy.writeValueAsBytes(new TextNode("hi")));

			CommunityDetectionResult completeTask = new CommunityDetectionResult();
			completeTask.setProgress(100);
			completeTask.setStatus(CommunityDetectionResult.COMPLETE_STATUS);
			CXMateResult cxMateRes = new CXMateResult();
			cxMateRes.setData(new TextNode("success"));
			completeTask.setResult(omappy.readTree(cxMateRes.asJson()));
            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
			CommunityDetectionEngine mockEngine = createMock(CommunityDetectionEngine.class);
			expect(mockEngine.getResult("12345")).andReturn(completeTask);
			
			Capture<CommunityDetectionRequest> cappy = Capture.newInstance();
			expect(mockEngine.request(capture(cappy))).andReturn("12345");
            replay(mockEngine);
			
            Configuration.getInstance().setCommunityDetectionEngine(mockEngine);
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CXMateResult cxRes = mapper.readValue(response.getOutput(),
                    CXMateResult.class);
            assertEquals(0, cxRes.getErrors().size());
			verify(mockEngine);
            CommunityDetectionRequest theRequest = cappy.getValue();
            assertEquals(null, theRequest.getCustomParameters());
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testLegacyDiffusionTaskSuccessWithQueryArgs() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
			fw.write(Configuration.DIFFUSION_POLLDELAY + " = 0\n");
			fw.write(Configuration.ALGORITHM_MAP + " = "
					+ TestDiffusion.
							writeConfigurationForDiffusion(tempDir.getAbsolutePath()) + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = getDispatcher();
            MockHttpRequest request = MockHttpRequest.post(Configuration.LEGACY_DIFFUSION_PATH
                    + "?time=0.1&normalize_laplacian=false&input_attribute_name=theinput&output_attribute_name=theoutput");
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);
            
            request.content(omappy.writeValueAsBytes(new TextNode("hi")));

			CommunityDetectionResult completeTask = new CommunityDetectionResult();
			completeTask.setProgress(100);
			completeTask.setStatus(CommunityDetectionResult.COMPLETE_STATUS);
			CXMateResult cxMateRes = new CXMateResult();
			cxMateRes.setData(new TextNode("success"));
			completeTask.setResult(omappy.readTree(cxMateRes.asJson()));
            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
			CommunityDetectionEngine mockEngine = createMock(CommunityDetectionEngine.class);
			expect(mockEngine.getResult("12345")).andReturn(completeTask);
			
			Capture<CommunityDetectionRequest> cappy = Capture.newInstance();
			expect(mockEngine.request(capture(cappy))).andReturn("12345");
            replay(mockEngine);
			
            Configuration.getInstance().setCommunityDetectionEngine(mockEngine);
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            CXMateResult cxRes = mapper.readValue(response.getOutput(),
                    CXMateResult.class);
            assertEquals(0, cxRes.getErrors().size());
			verify(mockEngine);
            CommunityDetectionRequest theRequest = cappy.getValue();
            assertEquals("0.1", theRequest.getCustomParameters().get("--time"));
            assertEquals("false", theRequest.getCustomParameters().get("--normalize_laplacian"));
            assertEquals("theinput", theRequest.getCustomParameters().get("--input_attribute_name"));
            assertEquals("theoutput", theRequest.getCustomParameters().get("--output_attribute_name"));
            
        } finally {
            _folder.delete();
        }
    }
}
