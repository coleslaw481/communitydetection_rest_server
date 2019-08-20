package org.ndexbio.communitydetection.rest.services; // Note your package will be {{ groupId }}.rest

import java.net.URI;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.servers.Server;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResult;
import org.ndexbio.communitydetection.rest.engine.CommunityDetectionEngine;
import org.ndexbio.communitydetection.rest.model.CommunityDetectionResultStatus;
import org.ndexbio.communitydetection.rest.model.ErrorResponse;
import org.ndexbio.communitydetection.rest.model.Task;
import org.ndexbio.communitydetection.rest.model.exceptions.CommunityDetectionException;

/**
 * CommunityDetection service
 * @author churas
 */
@OpenAPIDefinition( info = 
    @Info(title = "Community Detection REST service",
          version = "0.1.0",
          description = "This service lets caller invoke various community detection clustering algorithms")
)
@Server(
        description = "default",
        url = "/cd" + Configuration.APPLICATION_PATH
        )
@Path("/")
public class CommunityDetection {
    
    static Logger logger = LoggerFactory.getLogger(CommunityDetection.class);
    
    /**
     * Handles requests to do enrichment
     * @return {@link javax.ws.rs.core.Response} 
     */
    @POST 
    @Path(Configuration.V_ONE_PATH + "/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Submits Community Detection task",
               description="Payload in JSON format needs to have edgeList along with name of  algorithm to run "
                       + "and any algorithm specific parameters\n" +
"\n" +
"The service should upon post return 202 and set location to resource to poll for result. Which will\n" +
"Match the URL of GET request below.",
               responses = {
                   @ApiResponse(responseCode = "202",
                           description = "The task was successfully submitted to the service. Visit the URL "
                                   + "specified in Location field in HEADERS to status and results\n",
                           headers = @Header(name = "Location", description = "URL containing resource generated by this request"),
                           content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = Task.class))),
                   @ApiResponse(responseCode = "500", description = "Server Error",
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ErrorResponse.class)))
               })
    public Response request(@RequestBody(description="Request as json", required = true,
                                                   content = @Content(schema = @Schema(implementation = CommunityDetectionRequest.class))) final String query) {
        ObjectMapper omappy = new ObjectMapper();

        try {
            // not sure why but I cannot get resteasy and jackson to worktogether to
            // automatically translate json to Query class so I'm doing it after the
            // fact
            CommunityDetectionEngine engine = Configuration.getInstance().getCommunityDetectionEngine();
            if (engine == null){
                throw new NullPointerException("CommunityDetection Engine not loaded");
            }
            CommunityDetectionRequest pQuery = omappy.readValue(query, CommunityDetectionRequest.class);
            String id = engine.request(pQuery);
            if (id == null){
                throw new CommunityDetectionException("No id returned from CommunityDetection engine");
            }
            Task t = new Task();
            t.setId(id);
            return Response.status(202).location(new URI(Configuration.getInstance().getHostURL() +
                                                         Configuration.V_ONE_PATH + "/" + id).normalize()).entity(omappy.writeValueAsString(t)).build();
        } catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error requesting CommunityDetection", ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
        }
    }

    @GET 
    @Path(Configuration.V_ONE_PATH + "/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets result of Community Detection task",
               description="NOTE: For incomplete/failed jobs only Status, message, progress, and walltime will\n" +
"be returned in JSON",
               responses = {
                   @ApiResponse(responseCode = "200",
                           description = "Success",
                           content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = CommunityDetectionResult.class))),
                   @ApiResponse(responseCode = "410",
                           description = "Task not found"),
                   @ApiResponse(responseCode = "500", description = "Server Error",
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ErrorResponse.class)))
               })
    public Response getResult(@PathParam("id") final String id,
            @Parameter(description = "Starting index of result, should be an integer 0 or larger") @QueryParam("start") int start,
            @Parameter(description = "Number of results to return, 0 for all") @QueryParam("size") int size) {
        ObjectMapper omappy = new ObjectMapper();

        try {
            CommunityDetectionEngine engine = Configuration.getInstance().getCommunityDetectionEngine();
            if (engine == null){
                throw new NullPointerException("CommunityDetection Engine not loaded");
            }
            
            CommunityDetectionResult eqr = engine.getResult(id);
            if (eqr == null){
                return Response.status(410).build();
            }
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(omappy.writeValueAsString(eqr)).build();
        }
        catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error getting results for id: " + id, ex);
            return Response.status(500).type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
        }
    }

    @GET 
    @Path(Configuration.V_ONE_PATH + "/{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets status of Community Detection task",
               description="This lets caller get status without getting the full result back",
               responses = {
                   @ApiResponse(responseCode = "200",
                           description = "Success",
                           content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = CommunityDetectionResultStatus.class))),
                   @ApiResponse(responseCode = "410",
                           description = "Task not found"),
                   @ApiResponse(responseCode = "500", description = "Server Error",
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ErrorResponse.class)))
               })
    public Response getRequestStatus(@PathParam("id") final String id) {
        ObjectMapper omappy = new ObjectMapper();

        try {
            CommunityDetectionEngine engine = Configuration.getInstance().getCommunityDetectionEngine();
            if (engine == null){
                throw new NullPointerException("CommunityDetection Engine not loaded");
            }
            CommunityDetectionResultStatus eqs = engine.getStatus(id);
            if (eqs ==  null){
                return Response.status(410).build();
            }
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(omappy.writeValueAsString(eqs)).build();
        }
        catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error getting results for id: " + id, ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
        }
    }

    @DELETE 
    @Path(Configuration.V_ONE_PATH + "/{id}")
    @Operation(summary = "Deletes task associated with {id} passed in",
               description="",
               responses = {
                   @ApiResponse(responseCode = "200",
                           description = "Delete request successfully received"),
                   @ApiResponse(responseCode = "400",
                           description = "Invalid delete request"),
                   @ApiResponse(responseCode = "500", description = "Server Error",
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ErrorResponse.class)))
               })
    public Response deleteRequest(@PathParam("id") final String id) {
        ObjectMapper omappy = new ObjectMapper();

        try {
            CommunityDetectionEngine engine = Configuration.getInstance().getCommunityDetectionEngine();
            if (engine == null){
                throw new NullPointerException("CommunityDetection Engine not loaded");
            }
            engine.delete(id);
            return Response.ok().build();
        }
        catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error deleting: " + id, ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
        }
    }
}