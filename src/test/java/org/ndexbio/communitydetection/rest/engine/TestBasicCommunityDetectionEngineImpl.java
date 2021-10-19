package org.ndexbio.communitydetection.rest.engine;


import com.fasterxml.jackson.databind.node.TextNode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResult;
import org.ndexbio.communitydetection.rest.model.ServerStatus;


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
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task", "docker", null, null);
        engine.updateThreadSleepTime(1);
        engine.threadSleep();
    }
    
    @Test
    public void testRunWithShutDownTrue(){
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task", "docker", null, null);
        engine.shutdown();
        engine.run();
    }
    
    @Test
    public void testLogServerStatus(){
        // try with null
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task", "docker", null, null);
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
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task", "docker", null, null);
        String res = engine.getCommunityDetectionResultFilePath("12345");
        assertEquals("task/12345/" + CommunityDetectionEngineImpl.CDRESULT_JSON_FILE, res);
    }
    
    @Test
    public void testsaveCommunityDetectionResultToFilesystem() throws IOException {
        try {
            
            File tempDir = _folder.newFolder();
            CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, tempDir.getAbsolutePath(), "docker", null, null);
        
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
      
        CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, "task", "docker", null, null);
        
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
            CommunityDetectionEngineImpl engine = new CommunityDetectionEngineImpl(null, tempDir.getAbsolutePath(), "docker", null, null);
            
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
}