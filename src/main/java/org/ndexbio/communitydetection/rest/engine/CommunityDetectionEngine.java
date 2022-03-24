package org.ndexbio.communitydetection.rest.engine;

import java.io.InputStream;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithms;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResultStatus;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResult;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;
import org.ndexbio.communitydetection.rest.model.ServerStatus;

/**
 *
 * @author churas
 */
public interface CommunityDetectionEngine extends Runnable {
    
    /**
     * Submits request for processing
     * @param request to process
     * @throws CommunityDetectionException if there is an error
     * @return UUID as a string that is an identifier for query
     */
    public String request(CommunityDetectionRequest request) throws CommunityDetectionException;
     
    /**
     * Gets query results
     * @param id id of task

     * @return result of task
     * @throws CommunityDetectionException  if there is an error
     */
    public CommunityDetectionResult getResult(final String id) throws CommunityDetectionException;
	
	/**
     * Gets query result data only as {@code InputStream}
     * @param id id of task
     * @return data in result of task as {@code InputStream}
     * @throws CommunityDetectionException  if there is an error
     */
    public InputStream getResultData(final String id) throws CommunityDetectionException;
	
	/**
     * Gets mime type for result data
     * @param id id of task
     * @return mime type for result data ie image/png 
     * @throws CommunityDetectionException  if there is an error
     */
    public String getResultDataType(final String id) throws CommunityDetectionException;
    
    
    /**
     * Gets query status
     * @param id id of task
     * @return status of task
     * @throws CommunityDetectionException if there is an error
     */
    public CommunityDetectionResultStatus getStatus(final String id) throws CommunityDetectionException;
    
    /**
     * Deletes query
     * @param id id of task
     * @throws CommunityDetectionException if there is an error
     */
    public void delete(final String id) throws CommunityDetectionException;
 
    
    /**
     * Gets community detection algorithms supported by this service
     * @throws CommunityDetectionException if there is an error
     * @return algorithms
     */
    public CommunityDetectionAlgorithms getAlgorithms() throws CommunityDetectionException;
    
    /**
     * Gets status of server
     * @return status of server
     * @throws CommunityDetectionException if there is an error
     */
    public ServerStatus getServerStatus() throws CommunityDetectionException;
    
    /**
     * Tells implementing objects to shutdown
     */
    public void shutdown();
    
}
