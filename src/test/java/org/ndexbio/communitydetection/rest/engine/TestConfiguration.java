/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.communitydetection.rest.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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
                fail("Expected EnrichmentException");
            } catch(EnrichmentException ee){
                assertTrue(ee.getMessage().contains("FileNotFound Exception"));
            }
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testConfigurationAlternatePathNoMatchingProps() throws EnrichmentException, IOException {
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
            assertEquals("/tmp", config.getDatabaseDirectory());
            assertEquals("/tmp", config.getTaskDirectory());
            assertNull(config.getNDExDatabases());
            assertNull(config.getCommunityDetectionEngine());
            assertEquals(File.separator + "tmp" + File.separator + Configuration.DATABASE_RESULTS_JSON_FILE,
                         config.getDatabaseResultsFile().getAbsolutePath());
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testConfigurationValidConfiguration() throws EnrichmentException, IOException {
        File tempDir = _folder.newFolder();
        try {
            File configFile = new File(tempDir.getAbsolutePath() + File.separator + "conf");
            File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "tasks");
            Properties props = new Properties();
            props.setProperty(Configuration.DATABASE_DIR, tempDir.getAbsolutePath());
            props.setProperty(Configuration.TASK_DIR, taskDir.getAbsolutePath());
            props.setProperty(Configuration.NDEX_SERVER, "server");
            props.setProperty(Configuration.NDEX_USERAGENT, "agent");
            FileOutputStream fos = new FileOutputStream(configFile);
            props.store(fos, "hello");
            fos.flush();
            fos.close();
            Configuration.setAlternateConfigurationFile(configFile.getAbsolutePath());
            Configuration config = Configuration.reloadConfiguration();
            assertEquals(tempDir.getAbsolutePath(), config.getDatabaseDirectory());
            assertEquals(taskDir.getAbsolutePath(), config.getTaskDirectory());
            assertNull(config.getCommunityDetectionEngine());
            assertEquals(tempDir.getAbsolutePath() + File.separator + Configuration.DATABASE_RESULTS_JSON_FILE,
                         config.getDatabaseResultsFile().getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper();
            
            InternalDatabaseResults idr = new InternalDatabaseResults();
            idr.setUniverseUniqueGeneCount(10);
            mapper.writeValue(config.getDatabaseResultsFile(), idr);

            InternalDatabaseResults residr = config.getNDExDatabases();
            assertEquals(idr.getUniverseUniqueGeneCount(), residr.getUniverseUniqueGeneCount());
        } finally {
            _folder.delete();
        }
    }
}
