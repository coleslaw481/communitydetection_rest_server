package org.ndexbio.communitydetection.rest.services;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.servlet.ServletException;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.ndexbio.communitydetection.rest.engine.BasicCommunityDetectionEngineFactory;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ndexbio.communitydetection.rest.engine.CommunityDetectionEngine;
/**
 *
 * @author churas
 */
public class OpenApiHttpServletDispatcher extends HttpServletDispatcher {
    
    static Logger _logger = LoggerFactory.getLogger(OpenApiHttpServletDispatcher.class.getSimpleName());
    
    public OpenApiHttpServletDispatcher() throws CommunityDetectionException{
        super();
        _logger.info("In constructor");
       

    }

    @Override
    public void init(javax.servlet.ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        _logger.info("Entering init()");
        _logger.info("Exiting init()");
    }
    
    @Override
    public void destroy() {
        super.destroy();
        _logger.info("In destroy()");
    }
}
