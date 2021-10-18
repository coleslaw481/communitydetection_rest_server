package org.ndexbio.communitydetection.rest;

import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;
import org.ndexbio.communitydetection.rest.services.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters swagger open api documentation based on configuration passed into
 * this App
 * @author churas
 */
public class SwaggerFilter extends AbstractSpecFilter {

    static Logger _logger = LoggerFactory.getLogger(SwaggerFilter.class.getSimpleName());
    
    @Override
    public Optional<OpenAPI> filterOpenAPI(OpenAPI openAPI, Map<String,
            List<String>> params, Map<String, String> cookies,
            Map<String, List<String>> headers) {
        
        Info info = openAPI.getInfo();
        String desc = getSwaggerDescription();
        if (desc != null){
            info.setDescription(desc);
        }
        
        String title = getSwaggerTitle();
        if (title != null){
            info.setTitle(title);
        }
        
        String version = getVersion();
        if (version != null){
            info.setVersion(version);
        }
      
        List<Server> servers = openAPI.getServers();
        if (servers != null){
            String serverUrl = getServerUrl();
            if (serverUrl != null){
                //List<Server> newServers = new ArrayList<>();
                for (Server server: servers){
                    _logger.info("Found server: " + server.getUrl());
                    server.setUrl(serverUrl);
                }
            }
            //not sure if this is needed
            //openAPI.setServers(servers);
        }
        
        openAPI.setInfo(info);
        return Optional.of(openAPI);
    }
    
    private String getServerUrl(){
        try {
            return Configuration.getInstance().getSwaggerServer();
        } catch(CommunityDetectionException cde){
            _logger.warn("unable to get server for swagger", cde);
        }
        return null;
    }
    
    private String getSwaggerDescription(){
        try {
            return Configuration.getInstance().getSwaggerDescription();
        } catch(CommunityDetectionException cde){
             _logger.warn("unable to get swagger description", cde);
        }
        return null;
    }
    
    private String getSwaggerTitle(){
        try {
            return Configuration.getInstance().getSwaggerTitle();
        } catch(CommunityDetectionException cde){
             _logger.warn("unable to get swagger title", cde);
        }
        return null;
    }
    
    /**
     * Reads /META-INFO/MANIFEST.MF for version and build information
     * setting _version and _buildNumber to those values if found.
     */
    private String getVersion(){
        String jarPath = SwaggerFilter.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        JarFile jar = null;
        try {
            jar = new JarFile(jarPath);
            Manifest manifest = jar.getManifest();
            Attributes aa = manifest.getMainAttributes();	
            return aa.getValue("CommunityDetection-Version");
           
        } catch (IOException e) {
            _logger.error("failed to read MANIFEST.MF", e);
        } finally {
            
            if (jar != null){
                try {
                    jar.close();
                } catch(IOException io){
                    _logger.warn("Not a show stopper, but caught IOException closing jar", io);
                }
            }
        }
        return null;
    }
}
