package org.ndexbio.communitydetection.rest.engine;


import static org.easymock.EasyMock.expect;

import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithms;
import org.ndexbio.communitydetection.rest.model.ServerStatus;
import org.ndexbio.communitydetection.rest.services.Configuration;



/**
 *
 * @author churas
 */
public class TestBasicCommunityDetectionEngineFactory {
    

   
    @Test
    public void testGetCommunityDetectionEngine() throws Exception {

        Configuration mockConfig = mock(Configuration.class);
        expect(mockConfig.getNumberWorkers()).andReturn(5);
        expect(mockConfig.getTaskDirectory()).andReturn("/task");
        expect(mockConfig.getDockerCommand()).andReturn("/bin/docker");
        CommunityDetectionAlgorithms cdas = new CommunityDetectionAlgorithms();

        expect(mockConfig.getAlgorithms()).andReturn(cdas);
        replay(mockConfig);
        BasicCommunityDetectionEngineFactory factory = new BasicCommunityDetectionEngineFactory(mockConfig);
        CommunityDetectionEngine cde = factory.getCommunityDetectionEngine();

        verify(mockConfig);
        assertEquals(cdas, cde.getAlgorithms());
        ServerStatus ss = cde.getServerStatus();
        assertEquals(ServerStatus.OK_STATUS, ss.getStatus());
    }
}