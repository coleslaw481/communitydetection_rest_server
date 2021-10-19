package org.ndexbio.communitydetection.rest.engine.util;

import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithm;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest;
import org.ndexbio.communitydetection.rest.model.ErrorResponse;

/**
 *
 * @author churas
 */
public interface CommunityDetectionRequestValidator {
    
    /**
     * Validates the request 
     * @param cda Algorithm to run
     * @param cdr The request to validate
     * @return null upon success otherwise {@link org.ndexbio.communitydetection.rest.model.ErrorResponse} describing the error
     */
    public ErrorResponse validateRequest(CommunityDetectionAlgorithm cda, CommunityDetectionRequest cdr);
    
}
