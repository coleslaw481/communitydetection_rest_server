package org.ndexbio.communitydetection.rest.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.servers.Server;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResult;
import org.ndexbio.communitydetection.rest.engine.CommunityDetectionEngine;
import org.ndexbio.communitydetection.rest.model.CXMateResult;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithm;
import org.ndexbio.communitydetection.rest.model.ErrorResponse;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionBadRequestException;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;

/**
 * Diffusion service
 * @author churas
 */
@Path("/")
public class Diffusion {
    
    static Logger logger = LoggerFactory.getLogger(Diffusion.class);
    
	@GET
	@Path(Configuration.LEGACY_DIFFUSION_PATH + "/")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(hidden = true,
			   summary = "Legacy endpoint to let user know NOT to use this endpoint",
			   description="This endpoint exists to maintain consistency with legacy "
					   + "diffusion service that returns 405 error and json response",
			   responses = {
				   @ApiResponse(responseCode = "405", 
						        description = "The response will be JSON with no data "
										+ "and a single error denoting this endpoint "
										+ " should not be used.",
								content = @Content(mediaType = MediaType.APPLICATION_JSON,
								schema = @Schema(implementation = CXMateResult.class)))
			   })
	public Response legacyDiffusionGet(){
		CXMateResult result = new CXMateResult("you must use the POST method with this endpoint",
		null, Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), null);
		 return Response.status(Response.Status.METHOD_NOT_ALLOWED).type(MediaType.APPLICATION_JSON)
					.entity(result.asJson()).build();
	}
	
	@DELETE
	@Path(Configuration.LEGACY_DIFFUSION_PATH + "/")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(hidden = true,
			   summary = "Legacy endpoint to let user know NOT to use this endpoint",
			   description="This endpoint exists to maintain consistency with legacy "
					   + "diffusion service that returns 405 error and json response",
			   responses = {
				   @ApiResponse(responseCode = "405", 
						        description = "The response will be JSON with no data "
										+ "and a single error denoting this endpoint "
										+ " should not be used.",
								content = @Content(mediaType = MediaType.APPLICATION_JSON,
								schema = @Schema(implementation = CXMateResult.class)))
			   })
	public Response legacyDiffusionDelete(){
		CXMateResult result = new CXMateResult("you must use the POST method with this endpoint",
		null, Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), null);
		 return Response.status(Response.Status.METHOD_NOT_ALLOWED).type(MediaType.APPLICATION_JSON)
					.entity(result.asJson()).build();
	}
	
	@PUT
	@Path(Configuration.LEGACY_DIFFUSION_PATH + "/")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(hidden = true,
			   summary = "Legacy endpoint to let user know NOT to use this endpoint",
			   description="This endpoint exists to maintain consistency with legacy "
					   + "diffusion service that returns 405 error and json response",
			   responses = {
				   @ApiResponse(responseCode = "405", 
						        description = "The response will be JSON with no data "
										+ "and a single error denoting this endpoint "
										+ " should not be used.",
								content = @Content(mediaType = MediaType.APPLICATION_JSON,
								schema = @Schema(implementation = CXMateResult.class)))
			   })
	public Response legacyDiffusionPut(){
		CXMateResult result = new CXMateResult("you must use the POST method with this endpoint",
		null, Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), null);
		 return Response.status(Response.Status.METHOD_NOT_ALLOWED).type(MediaType.APPLICATION_JSON)
					.entity(result.asJson()).build();
	}
	
	@POST
	@Path(Configuration.LEGACY_DIFFUSION_PATH + "/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(hidden = false,
			   summary = "Legacy endpoint to run network propagation",
			   description="Payload must be a CX network containing the nodes, "
					   + "edges and nodeAttributes aspects, as described in Request Body below.",
			   responses = {
				   @ApiResponse(responseCode = "200", 
						        description = "The response body will contain a CX network "
										+ "containing the nodes, edges, and nodeAttributes aspects. "
										+ "Each node will have two associated attributes, "
										+ "output_attribute_name_rank and output_attribute_name_heat "
										+ "where output_attribute_name can be set via the query "
										+ "string parameters (e.g., diffusion_output_rank and "
										+ "diffusion_output_heat). The _heat attribute will "
										+ "contain the heat of the node after diffusion. The _rank "
										+ "attribute will have the rank of the node relative to the "
										+ "heats of all other nodes in the network, starting with 0 "
										+ "as the hottest node.\n" + "\n"
										+ "Note that while _rank and _heat attributes "
										+ "will be returned for each node in the CX network, "
										+ "attributes present in the input network and not "
										+ "related to heat_diffusion are not guaranteed to be returned.",
								content = @Content(mediaType = MediaType.APPLICATION_JSON,
								schema = @Schema(implementation = CXMateResult.class))),
				   @ApiResponse(responseCode = "500",
						        description = "There was an error parsing input or running task",
				                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                  schema = @Schema(implementation = CXMateResult.class)))
			   })
	public Response legacyDiffusion(@RequestBody(description="The body of the request must be a CX network "
			+ "containing the nodes, edges, and nodeAttributes aspects. There must exist at least one "
			+ "nodeAttribute with a key name that matches the input_attribute_name parameter and holds "
			+ "a double, which will be interepreted as the heat of that node. (This condition can be "
			+ "minimally fulfilled by omitting the input_attribute_name parameter, and having at "
			+ "least one node with an attribute named diffusion_input with value 1.0.)\n" +
"\n" +
"All nodes that do not have this nodeAttribute set will be treated as having zero heat.", required = true) final String cxdata,
			@Parameter(description = "The upper bound on the exponential multiplication performed by diffusion", example="0.1") @QueryParam("time") Double time,
			@Parameter(description = "If True, will create a normalized laplacian matrix for diffusion") @QueryParam("normalize_laplacian") Boolean normalizeLaplacian,
			@Parameter(description = "The key diffusion will use to search for heats in the node attributes") @QueryParam("input_attribute_name") final String inputAttributeName,
			@Parameter(description = "Will be the prefix of the _rank and _heat attributes created by diffusion") @QueryParam("output_attribute_name") final String outputAttributeName) {
		
		
		ObjectMapper omapper = new ObjectMapper();
		// Create CommunityDetectionRequest passing cxdata in "data" field
		CommunityDetectionRequest cdr = new CommunityDetectionRequest();
		
		Map<String, String> customParams = new HashMap<>();
		if (time != null){
			customParams.put("--time", time.toString());
		}
		if (normalizeLaplacian != null){
			customParams.put("--normalize_laplacian", normalizeLaplacian.toString());
		}
		if (inputAttributeName != null){
			customParams.put("--input_attribute_name", inputAttributeName);
		}
		if (outputAttributeName != null){
			customParams.put("--output_attribute_name", outputAttributeName);
		}
		if (customParams.isEmpty() == false){
			cdr.setCustomParameters(customParams);
		}
		try {
			CommunityDetectionAlgorithm cda = Configuration.getInstance().getDiffusionAlgorithm();
			if (cda == null){
				throw new NullPointerException("No diffusion algorithm found");
			}
			cdr.setAlgorithm(cda.getName());
			cdr.setData(omapper.readTree(cxdata));
			
			CommunityDetectionEngine engine = Configuration.getInstance().getCommunityDetectionEngine();
				if (engine == null){
					throw new NullPointerException("CommunityDetection Engine not loaded");
				}
			// Submit task to service
			String id = engine.request(cdr);
			if (id == null){
					throw new CommunityDetectionException("No id returned from CommunityDetection Engine");
			}
			
			long pollingDelay = Configuration.getInstance().getDiffusionPollingDelay();
			// Wait for completion by invoking status once per second
			CommunityDetectionResult cRes = engine.getResult(id);
			while (cRes.getProgress() < 100){				
				Thread.sleep(pollingDelay);
				cRes = engine.getResult(id);
			}
			Response.Status taskStatus = Response.Status.INTERNAL_SERVER_ERROR;
			// check if successful otherwise leave status as failed
			if (cRes.getStatus().equals(CommunityDetectionResult.COMPLETE_STATUS)){
				taskStatus = Response.Status.OK;
			}
            return Response.status(taskStatus).type(MediaType.APPLICATION_JSON).entity(omapper.writeValueAsString(cRes.getResult())).build();
		} catch(JsonProcessingException jpe){
			CXMateResult er = new CXMateResult("Error parsing input data", cdr.getAlgorithm(),
			Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), jpe);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
		} catch(CommunityDetectionBadRequestException breq){
            ErrorResponse er = breq.getErrorResponse();
            if (er == null){
                er = new ErrorResponse("Bad request received", breq);
            }
			CXMateResult cError = new CXMateResult(cdr.getAlgorithm(), er);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(cError.asJson()).build();
		} catch(Exception ex){
			CXMateResult er = new CXMateResult("Error running diffusion",
					cdr.getAlgorithm(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
					ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
        }
	}
}