package org.ndexbio.communitydetection.rest;

import io.swagger.v3.oas.models.OpenAPI;
import static org.easymock.EasyMock.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.Assert.assertNotNull;
import org.junit.Rule;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.communitydetection.rest.services.Configuration;



/**
 *
 * @author churas
 */
public class TestSwaggerFilter {
  
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    @Test
    public void testfilterOpenAPIAllgetServersReturnsNullAndNoConfiguration() {
        OpenAPI mockOpenAPI = mock(OpenAPI.class);
        
        Info info = new Info();
        info.setTitle("title");
        info.setDescription("description");
        info.setTermsOfService("tos");
        info.setContact(null);
        info.setLicense(null);
        info.setVersion("version");
        expect(mockOpenAPI.getInfo()).andReturn(info);
        expect(mockOpenAPI.getServers()).andReturn(null);
        
        mockOpenAPI.setInfo(info);
        replay(mockOpenAPI);
        SwaggerFilter sf = new SwaggerFilter();
        Optional<OpenAPI> res = sf.filterOpenAPI(mockOpenAPI, null, null, null);
        verify(mockOpenAPI);
        
        assertNotNull(res.get());
    }
    
    @Test
    public void testfilterOpenAPI() throws IOException {
        
        try {
            
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.SWAGGER_TITLE + " = swagger_title\n");
            fw.write(Configuration.SWAGGER_DESC + " = swagger_description\n");
            fw.flush();
            fw.close();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            OpenAPI mockOpenAPI = mock(OpenAPI.class);

            Info info = new Info();
            info.setTitle("title");
            info.setDescription("description");
            info.setTermsOfService("tos");
            info.setContact(null);
            info.setLicense(null);
            info.setVersion("version");
            expect(mockOpenAPI.getInfo()).andReturn(info);
            List<Server> servers = new ArrayList<>();
            Server sOne = new Server();
            sOne.setUrl("url1");
            servers.add(sOne);
            Server sTwo = new Server();
            sTwo.setUrl("url2");
            servers.add(sTwo);
            expect(mockOpenAPI.getServers()).andReturn(servers);

            Info updatedInfo = new Info();
            updatedInfo.setTitle("swagger_title");
            updatedInfo.setDescription("swagger_description");
            updatedInfo.setTermsOfService("tos");
            updatedInfo.setContact(null);
            updatedInfo.setLicense(null);
            updatedInfo.setVersion("version");
            
            mockOpenAPI.setInfo(updatedInfo);
            replay(mockOpenAPI);
            SwaggerFilter sf = new SwaggerFilter();
            Optional<OpenAPI> res = sf.filterOpenAPI(mockOpenAPI, null, null, null);
            verify(mockOpenAPI);
            assertNotNull(res.get());
        } finally {
            _folder.delete();
        }
   
    }
}
