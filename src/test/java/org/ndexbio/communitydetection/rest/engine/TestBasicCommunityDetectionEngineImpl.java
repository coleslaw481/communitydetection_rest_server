package org.ndexbio.communitydetection.rest.engine;


import com.fasterxml.jackson.databind.node.TextNode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import org.easymock.Capture;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.communitydetection.rest.engine.util.CommunityDetectionRequestValidator;
import org.ndexbio.communitydetection.rest.engine.util.DockerCommunityDetectionRunner;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithm;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithms;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResult;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResultStatus;
import org.ndexbio.communitydetection.rest.model.ErrorResponse;
import org.ndexbio.communitydetection.rest.model.ServerStatus;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionBadRequestException;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;
import org.ndexbio.communitydetection.rest.services.Configuration;


/**
 *
 * @author churas
 */
public class TestBasicCommunityDetectionEngineImpl {
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    
    public TestBasicCommunityDetectionEngineImpl() {
    }
   
    @Test
    public void testthreadSleep() throws Exception {
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task",
                "docker", null, null);
        engine.updateThreadSleepTime(1);
        engine.threadSleep();
    }
    
    @Test
    public void testRunWithShutDownTrue(){
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task",
                "docker", null, null);
        engine.shutdown();
        engine.run();
    }
    
    @Test
    public void testLogServerStatus(){
        // try with null
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task",
                "docker", null, null);
        engine.logServerStatus(null);
        
        // try with empty ServerStatus
        ServerStatus ss = new ServerStatus();
        engine.logServerStatus(ss);
        
        // try with string fields set
        ss.setRestVersion("version");
        ss.setStatus("status");
        engine.logServerStatus(ss);
        
    }
    
    @Test
    public void testgetCommunityDetectionResultFilePath(){
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task",
                "docker", null, null);
        String res = engine.getCommunityDetectionResultFilePath("12345");
        assertEquals("task/12345/" + CommunityDetectionEngineImpl.CDRESULT_JSON_FILE, res);
    }
    
    @Test
    public void testsaveCommunityDetectionResultToFilesystem() throws IOException {
        try {
            
            File tempDir = _folder.newFolder();
            CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
        
            //try with null 1st
            engine.saveCommunityDetectionResultToFilesystem(null);
            
            CommunityDetectionResult cdr = new CommunityDetectionResult();
            cdr.setId("1");
            cdr.setResult(TextNode.valueOf("hi"));
            cdr.setMessage("message");
            
            //try where dest directory does not exist
            engine.saveCommunityDetectionResultToFilesystem(cdr);
            
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + cdr.getId());
            assertTrue(taskDir.mkdirs());

            engine.saveCommunityDetectionResultToFilesystem(cdr);
            
            CommunityDetectionResult cRes = engine.getCommunityDetectionResultFromDbOrFilesystem(cdr.getId());
            assertEquals("message", cRes.getMessage());
            
        }
        finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testlogResult() throws IOException {
      
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task",
                "docker", null, null);
        
        //try passing null
        engine.logResult(null);
        
        //try passing empty
        engine.logResult(new CommunityDetectionResult());
        
        //try passing fully set
        CommunityDetectionResult cdr = new CommunityDetectionResult();
        cdr.setId("1");
        cdr.setMessage("message");
        cdr.setProgress(50);
        cdr.setResult(TextNode.valueOf("hi"));
        cdr.setStartTime(1);
        cdr.setStatus("status");
        cdr.setWallTime(2);
        engine.logResult(cdr);
    }
    
    @Test
    public void testgetCommunityDetectionResultFromDbOrFilesystem() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            
            //try with invalid id
            assertNull(engine.getCommunityDetectionResultFromDbOrFilesystem("1"));
            
            //try with invalid data
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "1");
            assertTrue(taskDir.mkdirs());
            
            FileWriter fw = new FileWriter(engine.getCommunityDetectionResultFilePath("1"));
            fw.write("xxx");
            fw.flush();
            fw.close();
            assertNull(engine.getCommunityDetectionResultFromDbOrFilesystem("1"));
            
            File resFile = new File(engine.getCommunityDetectionResultFilePath("1"));
            assertTrue(resFile.delete());
            CommunityDetectionResult cdr = new CommunityDetectionResult();
            cdr.setId("1");
            cdr.setMessage("message");
            engine.saveCommunityDetectionResultToFilesystem(cdr);
            CommunityDetectionResult cRes = engine.getCommunityDetectionResultFromDbOrFilesystem("1");
            assertEquals("message", cRes.getMessage());
            
            
        } finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testgetResult() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            
            //try with null
            try {
                engine.getResult(null);
                fail("Expected CommunityDetectionException");
            } catch(CommunityDetectionException cde){
                assertEquals("Id is null", cde.getMessage());
            }
            
            //try with invalid id
            try {
                engine.getResult("1");
                fail("Expected CommunityDetectionException");
            } catch(CommunityDetectionException cde){
                assertEquals("No task with id of 1 found", cde.getMessage());
            }
            
            //try with invalid data
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "1");
            assertTrue(taskDir.mkdirs());
            
            
            File resFile = new File(engine.getCommunityDetectionResultFilePath("1"));
           
            CommunityDetectionResult cdr = new CommunityDetectionResult();
            cdr.setId("1");
            cdr.setMessage("message");
            engine.saveCommunityDetectionResultToFilesystem(cdr);
            try {
                CommunityDetectionResult res = engine.getResult("1");
                assertEquals("message", res.getMessage());
            } catch(CommunityDetectionException cde){
                fail("Unexpected CommunityDetectionException " + cde.getMessage());
            }
        } finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testgetStatus() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            
            //try with null
            try {
                engine.getStatus(null);
                fail("Expected CommunityDetectionException");
            } catch(CommunityDetectionException cde){
                assertEquals("Id is null", cde.getMessage());
            }
            
            //try with invalid id
            try {
                engine.getStatus("1");
                fail("Expected CommunityDetectionException");
            } catch(CommunityDetectionException cde){
                assertEquals("No task with id of 1 found", cde.getMessage());
            }
            
            //try with invalid data
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "1");
            assertTrue(taskDir.mkdirs());
            
            
            File resFile = new File(engine.getCommunityDetectionResultFilePath("1"));
           
            CommunityDetectionResult cdr = new CommunityDetectionResult();
            cdr.setId("1");
            cdr.setMessage("message");
            engine.saveCommunityDetectionResultToFilesystem(cdr);
            try {
                CommunityDetectionResultStatus res = engine.getStatus("1");
                assertEquals("message", res.getMessage());
            } catch(CommunityDetectionException cde){
                fail("Unexpected CommunityDetectionException " + cde.getMessage());
            }
        } finally {
            _folder.delete();
        } 
    }
    
    @Test
    public void testRequestWhereRequestIsNull(){
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task",
                "docker", null, null);
        try {
            engine.request(null);
            fail("Expected CommunityDetectionBadRequestException");
        } catch(CommunityDetectionBadRequestException cdbe){
            assertEquals("Request is null", cdbe.getMessage());
        } catch(CommunityDetectionException cde){
            fail("Unexpected exception: " + cde.getMessage());
        }
    }
    
    @Test
    public void testRequestNoAlgorithm(){
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task",
                "docker", null, null);
        try {
            CommunityDetectionRequest cdr = new CommunityDetectionRequest();
            
            engine.request(cdr);
            fail("Expected CommunityDetectionBadRequestException");
        } catch(CommunityDetectionBadRequestException cdbe){
            assertEquals("No algorithm specified", cdbe.getMessage());
        } catch(CommunityDetectionException cde){
            fail("Unexpected exception: " + cde.getMessage());
        }
    }
    
    @Test
    public void testRequestNoAlgorithmsSetInConstructor(){
        
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task",
                "docker", null, null);
        try {
            CommunityDetectionRequest cdr = new CommunityDetectionRequest();
            cdr.setAlgorithm("foo");
            engine.request(cdr);
            fail("Expected CommunityDetectionException");
        } catch(CommunityDetectionBadRequestException cdbe){
            fail("Unexpected exception: " + cdbe.getMessage());
            
        } catch(CommunityDetectionException cde){
            assertEquals("No algorithms are available to run in service", cde.getMessage());
        }
    }
    
    @Test
    public void testRequestNoAlgorithmsMatch(){
        CommunityDetectionAlgorithms algos = new CommunityDetectionAlgorithms();
        CommunityDetectionAlgorithm cda = new CommunityDetectionAlgorithm();
        cda.setName("blah");
        LinkedHashMap<String, CommunityDetectionAlgorithm> aMap = new LinkedHashMap<>();
        aMap.put(cda.getName(), cda);
        algos.setAlgorithms(aMap);
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task",
                "docker", algos, null);
        try {
            CommunityDetectionRequest cdr = new CommunityDetectionRequest();
            cdr.setAlgorithm("foo");
            engine.request(cdr);
            fail("Expected CommunityDetectionBadRequestException");
        } catch(CommunityDetectionBadRequestException cdbe){
            assertEquals("foo is not a valid algorithm", cdbe.getMessage());
        } catch(CommunityDetectionException cde){
            fail("Unexpected exception: " + cde.getMessage());
            
        }
    }
    
    @Test
    public void testRequestValidationFails(){
        CommunityDetectionAlgorithms algos = new CommunityDetectionAlgorithms();
        CommunityDetectionAlgorithm cda = new CommunityDetectionAlgorithm();
        cda.setName("foo");
        LinkedHashMap<String, CommunityDetectionAlgorithm> aMap = new LinkedHashMap<>();
        aMap.put(cda.getName(), cda);
        algos.setAlgorithms(aMap);
        CommunityDetectionRequestValidator mockValidator = mock(CommunityDetectionRequestValidator.class);
        CommunityDetectionRequest cdr = new CommunityDetectionRequest();
        cdr.setAlgorithm("foo");
        ErrorResponse er = new ErrorResponse();
        er.setMessage("problem");
        expect(mockValidator.validateRequest(cda, cdr)).andReturn(er);
        replay(mockValidator);
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task", 
                "docker", algos, mockValidator);
        try {
            engine.request(cdr);
            fail("Expected CommunityDetectionBadRequestException");
        } catch(CommunityDetectionBadRequestException cdbe){
            assertEquals("Validation failed", cdbe.getMessage());
            assertEquals("problem", er.getMessage());
        } catch(CommunityDetectionException cde){
            fail("Unexpected exception: " + cde.getMessage());
        }
        verify(mockValidator);
    }
    
    @Test
    public void testRequestTaskCreationFails() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.MOUNT_OPTIONS + " = :ro,z\n");
            fw.write(Configuration.ALGORITHM_TIMEOUT + " = 10\n");
            
            fw.flush();
            fw.close();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            CommunityDetectionAlgorithms algos = new CommunityDetectionAlgorithms();
            CommunityDetectionAlgorithm cda = new CommunityDetectionAlgorithm();
            cda.setName("foo");
            LinkedHashMap<String, CommunityDetectionAlgorithm> aMap = new LinkedHashMap<>();
            aMap.put(cda.getName(), cda);
            algos.setAlgorithms(aMap);
            CommunityDetectionRequestValidator mockValidator = mock(CommunityDetectionRequestValidator.class);
            CommunityDetectionRequest cdr = new CommunityDetectionRequest();
            cdr.setAlgorithm("foo");

            expect(mockValidator.validateRequest(cda, cdr)).andReturn(null);

            ExecutorService mockES = mock(ExecutorService.class);
            Capture<DockerCommunityDetectionRunner> cappy = Capture.newInstance();
            expect(mockES.submit(capture(cappy))).andThrow(new RejectedExecutionException("failed"));
            replay(mockES);
            replay(mockValidator);
            CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(mockES,
                    tempDir.getAbsolutePath(), "docker", algos, mockValidator);
            try {
                engine.request(cdr);
                fail("Expected CommunityDetectionException");
            } catch(CommunityDetectionBadRequestException cdbe){
                fail("Unexpected exception: " + cdbe.getMessage());
            } catch(CommunityDetectionException cde){
                assertEquals("failed", cde.getMessage());
            }
            
            assertNotNull(cappy.getValue());
            verify(mockValidator);
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testRequestSuccess() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.MOUNT_OPTIONS + " = :ro,z\n");
            fw.write(Configuration.ALGORITHM_TIMEOUT + " = 10\n");
            
            fw.flush();
            fw.close();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            CommunityDetectionAlgorithms algos = new CommunityDetectionAlgorithms();
            CommunityDetectionAlgorithm cda = new CommunityDetectionAlgorithm();
            cda.setName("foo");
            LinkedHashMap<String, CommunityDetectionAlgorithm> aMap = new LinkedHashMap<>();
            aMap.put(cda.getName(), cda);
            algos.setAlgorithms(aMap);
            CommunityDetectionRequestValidator mockValidator = mock(CommunityDetectionRequestValidator.class);
            CommunityDetectionRequest cdr = new CommunityDetectionRequest();
            cdr.setAlgorithm("foo");
            cdr.setData(TextNode.valueOf("hi"));
            Map<String, String> cParams = new HashMap<>();
            cParams.put("key1", null);
            cParams.put("key2", "val2");
            cdr.setCustomParameters(cParams);

            expect(mockValidator.validateRequest(cda, cdr)).andReturn(null);

            ExecutorService mockES = mock(ExecutorService.class);
            Capture<DockerCommunityDetectionRunner> cappy = Capture.newInstance();
            FutureTask<CommunityDetectionResult> mockFT = mock(FutureTask.class);
            expect(mockES.submit(capture(cappy))).andReturn(mockFT);
            replay(mockFT);
            replay(mockES);
            replay(mockValidator);
            CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(mockES,
                    tempDir.getAbsolutePath(), "docker", algos, mockValidator);
            try {
                assertNotNull(engine.request(cdr));
            } catch(CommunityDetectionBadRequestException cdbe){
                fail("Unexpected exception: " + cdbe.getMessage());
            } catch(CommunityDetectionException cde){
                fail("Unexpected exception: " + cde.getMessage());
            }
            
            assertNotNull(cappy.getValue());
            verify(mockValidator);
            verify(mockES);
            verify(mockFT);
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testDeleteNullId() throws IOException {
        try {
            File tempDir = _folder.newFolder();
             CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            try {
                engine.delete(null);
                fail("Expected CommunityDetectionException ");
            } catch(CommunityDetectionException cde){
                assertEquals("id is null", cde.getMessage());
            }
            
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testDeleteWhereIdNotInResultsOrFilesystem() throws IOException {
        try {
            File tempDir = _folder.newFolder();
             CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            try {
                engine.delete("1");
                
            } catch(CommunityDetectionException cde){
                fail("unexpected CommunityDetectionException: " + cde.getMessage());
            }
            
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testDeleteWhereIdIsInFilesystem() throws IOException {
        try {
            File tempDir = _folder.newFolder();
            
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "1");
            assertTrue(taskDir.mkdirs());
            
            FileWriter fw = new FileWriter(taskDir.getAbsolutePath()
                    + File.separator + CommunityDetectionEngineImpl.CDRESULT_JSON_FILE);
            fw.write("haha");
            fw.flush();
            fw.close();
            
             CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null,
                    tempDir.getAbsolutePath(), "docker", null, null);
            try {
                engine.delete("1");
                assertFalse(taskDir.exists());
            } catch(CommunityDetectionException cde){
                fail("unexpected CommunityDetectionException: " + cde.getMessage());
            }
            
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testGetAlgorithmsWhereAlgorithmsIsNull(){
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null,
                    "task", "docker", null, null);
        try {
            engine.getAlgorithms();
            fail("Expected CommunityDetectionException: ");
                
        } catch(CommunityDetectionException cde){
            assertEquals("No algorithms found", cde.getMessage());
        }
    }
}