package org.ndexbio.communitydetection.rest.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Properties;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithm;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithms;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;
import org.ndexbio.communitydetection.rest.services.Configuration;

/**
 *
 * @author churas
 */
public class TestConfiguration {
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    
    @Test
    public void testConfigurationNoConfigurationFound() throws IOException {
        File tempDir = _folder.newFolder();
        try {
            File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
           
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            try {
                Configuration config = Configuration.reloadConfiguration();
                fail("Expected CommunityDetectionException");
            } catch(CommunityDetectionException ee){
                assertTrue(ee.getMessage().contains("FileNotFound Exception"));
            }
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testConfigurationConfigurationReadPermissionError() throws IOException {
        File tempDir = _folder.newFolder();
        File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
        try {
            Properties props = new Properties();
            props.setProperty("foo", "hello");
            FileOutputStream fos = new FileOutputStream(configFile);
            props.store(fos, "hello");
            fos.flush();
            fos.close();
            configFile.setReadable(false);
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            try {
                Configuration config = Configuration.reloadConfiguration();
                fail("Expected CommunityDetectionException");
            } catch(CommunityDetectionException ee){
                assertTrue(ee.getMessage().contains("FileNotFound Exception"));
            }
        } finally {
            configFile.setReadable(true);
            _folder.delete();
        }
    }
    
    @Test
    public void testConfigurationAlternatePathNoMatchingProps() throws CommunityDetectionException, IOException {
        File tempDir = _folder.newFolder();
        try {
            File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
            Properties props = new Properties();
            props.setProperty("foo", "hello");
            FileOutputStream fos = new FileOutputStream(configFile);
            props.store(fos, "hello");
            fos.flush();
            fos.close();
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            Configuration config = Configuration.reloadConfiguration();
            assertEquals("/tmp", config.getTaskDirectory());
            assertNull(config.getCommunityDetectionEngine());
            assertEquals(1, config.getNumberWorkers());
            assertEquals("docker", config.getDockerCommand());
            assertEquals(null, config.getDiffusionAlgorithm());
            assertEquals(100, config.getDiffusionPollingDelay());
            assertEquals(":ro", config.getMountOptions());
            assertNull(config.getSwaggerTitle());
            assertNull(config.getSwaggerDescription());
            assertEquals("/cd", config.getRunServerContextPath());
            assertEquals("/communitydetection", config.getRunServerApplicationPath());
            assertEquals("/cd/communitydetection", config.getSwaggerServer());
            
            
            assertEquals(null, config.getAlgorithms());
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testConfigurationValidConfiguration() throws CommunityDetectionException, IOException {
        File tempDir = _folder.newFolder();
        try {
            File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "tasks");
            Properties props = new Properties();
            props.setProperty(Configuration.TASK_DIR, taskDir.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(configFile);
            props.store(fos, "hello");
            fos.flush();
            fos.close();
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            Configuration config = Configuration.reloadConfiguration();
            assertEquals(taskDir.getAbsolutePath(), config.getTaskDirectory());
            assertNull(config.getCommunityDetectionEngine());
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testParseAlgorithmMapInvalidData() throws CommunityDetectionException, IOException {
        File tempDir = _folder.newFolder();
        try {
            File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "tasks");
            Properties props = new Properties();
            props.setProperty(Configuration.TASK_DIR, taskDir.getAbsolutePath());
            props.setProperty(Configuration.ALGORITHM_MAP, "haha");
            FileOutputStream fos = new FileOutputStream(configFile);
            props.store(fos, "hello");
            fos.flush();
            fos.close();
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            Configuration config = Configuration.reloadConfiguration();
            assertEquals(taskDir.getAbsolutePath(), config.getTaskDirectory());
            assertNull(config.getCommunityDetectionEngine());
            assertEquals(180, config.getAlgorithmTimeOut());
            assertEquals(null, config.getAlgorithms());
        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testParseAlgorithmMapNoMatchingDiffusion() throws CommunityDetectionException, IOException {
        File tempDir = _folder.newFolder();
        try {
            File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "tasks");
            File algoConfig = new File(tempDir.getAbsolutePath() + File.separator + "algo.json");
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
            oMapper.writeValue(algoConfig, cdAlgos);
			
			
            Properties props = new Properties();
            props.setProperty(Configuration.TASK_DIR, taskDir.getAbsolutePath());
            props.setProperty(Configuration.ALGORITHM_MAP, algoConfig.getAbsolutePath());
            props.setProperty(Configuration.DIFFUSION_ALGO, "anotherdiffusion");
            FileOutputStream fos = new FileOutputStream(configFile);
            props.store(fos, "hello");
            fos.flush();
            fos.close();
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            Configuration config = Configuration.reloadConfiguration();
            assertEquals(taskDir.getAbsolutePath(), config.getTaskDirectory());
            assertNull(config.getCommunityDetectionEngine());
            assertEquals(180, config.getAlgorithmTimeOut());
            CommunityDetectionAlgorithms resAlgos = config.getAlgorithms();
            assertEquals(2, resAlgos.getAlgorithms().size());
            assertNull(config.getDiffusionAlgorithm());
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testParseAlgorithmMapWithDiffusion() throws CommunityDetectionException, IOException {
        File tempDir = _folder.newFolder();
        try {
            File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "tasks");
            File algoConfig = new File(tempDir.getAbsolutePath() + File.separator + "algo.json");
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
            oMapper.writeValue(algoConfig, cdAlgos);
			
			
            Properties props = new Properties();
            props.setProperty(Configuration.TASK_DIR, taskDir.getAbsolutePath());
            props.setProperty(Configuration.ALGORITHM_MAP, algoConfig.getAbsolutePath());
            props.setProperty(Configuration.DIFFUSION_ALGO, "legacydiffusion");
            FileOutputStream fos = new FileOutputStream(configFile);
            props.store(fos, "hello");
            fos.flush();
            fos.close();
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            Configuration config = Configuration.reloadConfiguration();
            assertEquals(taskDir.getAbsolutePath(), config.getTaskDirectory());
            assertNull(config.getCommunityDetectionEngine());
            assertEquals(180, config.getAlgorithmTimeOut());
            CommunityDetectionAlgorithms resAlgos = config.getAlgorithms();
            assertEquals(2, resAlgos.getAlgorithms().size());
            CommunityDetectionAlgorithm dAlgo = config.getDiffusionAlgorithm();
            assertEquals("legacydiffusion", dAlgo.getName());
        } finally {
            _folder.delete();
        }
    }
}
