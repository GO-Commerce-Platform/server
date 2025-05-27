package dev.tiodati.saas.gocommerce.platform.api;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.api.dto.StoreResponse;
import dev.tiodati.saas.gocommerce.platform.service.PlatformAdminService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/platform/stores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Platform Administration", description = "Endpoints for platform-level store management")
public class PlatformAdminResource {

    private final PlatformAdminService platformAdminService;

    @Inject
    public PlatformAdminResource(PlatformAdminService platformAdminService) {
        this.platformAdminService = platformAdminService;
    }

    @POST
    @RolesAllowed("Platform Admin")
    @Operation(summary = "Create a new store", description = "Creates a new store in the platform with the specified information")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Store successfully created"),
            @APIResponse(responseCode = "400", description = "Invalid input data"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions to create a store"),
            @APIResponse(responseCode = "409", description = "Store with the given subdomain already exists")
    })
    public Response createStore(@Valid CreateStoreRequest request) {
        StoreResponse createdStore = platformAdminService.createStore(request);
        return Response.status(Response.Status.CREATED).entity(createdStore).build();
    }
}
