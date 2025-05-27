package dev.tiodati.saas.gocommerce.store.resource;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import io.quarkus.logging.Log;

import dev.tiodati.saas.gocommerce.auth.annotation.RequiresStoreRole;
import dev.tiodati.saas.gocommerce.resource.dto.CreateStoreDto;
import dev.tiodati.saas.gocommerce.resource.dto.StoreDto;
import dev.tiodati.saas.gocommerce.store.model.Store;
import dev.tiodati.saas.gocommerce.store.model.StoreAdmin;
import dev.tiodati.saas.gocommerce.store.model.StoreStatus;
import dev.tiodati.saas.gocommerce.store.service.StoreService;
import dev.tiodati.saas.gocommerce.util.PasswordHashingUtil;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/admin/stores")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Store Management", description = "Operations for managing stores")
@RolesAllowed("admin") // Base restriction - only admins can access store managemen
public class StoreResource {

    // private static final Logger LOG = Logger.getLogger(StoreResource.class);

    private final StoreService storeService;
    private final PasswordHashingUtil passwordHashingUtil;

    @Inject
    public StoreResource(StoreService storeService, PasswordHashingUtil passwordHashingUtil) {
        this.storeService = storeService;
        this.passwordHashingUtil = passwordHashingUtil;
    }

    @GET
    @Operation(summary = "List all stores", description = "Returns a list of all stores")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "List of stores retrieved successfully", content = @Content(schema = @Schema(implementation = StoreDto.class)))
    })
    public List<StoreDto> listStores() {
        return storeService.listAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{ id }")
    @Operation(summary = "Get store by ID", description = "Returns a store by its ID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Store retrieved successfully", content = @Content(schema = @Schema(implementation = StoreDto.class))),
            @APIResponse(responseCode = "404", description = "Store not found")
    })
    public Response getStore(@PathParam("id") UUID id) {
        return storeService.findById(id)
                .map(this::convertToDto)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Operation(summary = "Create a new store", description = "Creates a new store with an admin user")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Store created successfully"),
            @APIResponse(responseCode = "400", description = "Invalid input"),
            @APIResponse(responseCode = "409", description = "Store key or subdomain already exists")
    })
    public Response createStore(@Valid CreateStoreDto createStoreDto) {
        // Check if store key or subdomain already exists
        if (storeService.findByStoreKey(createStoreDto.storeKey()).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Store key already exists"))
                    .build();
        }

        if (storeService.findBySubdomain(createStoreDto.subdomain()).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Subdomain already exists"))
                    .build();
        }

        try {
            Store store = new Store();
            store.setName(createStoreDto.name());
            store.setStoreKey(createStoreDto.storeKey());
            store.setSubdomain(createStoreDto.subdomain());
            store.setStatus(StoreStatus.valueOf("PENDING")); // Default status

            StoreAdmin admin = new StoreAdmin();
            admin.setUsername(createStoreDto.adminUsername());
            admin.setEmail(createStoreDto.adminEmail());
            admin.setPasswordHash(passwordHashingUtil.hashPassword(createStoreDto.adminPassword()));

            Store createdStore = storeService.createStore(store, admin);
            return Response.created(URI.create("/api/admin/stores/" + createdStore.getId()))
                    .entity(convertToDto(createdStore)).build();
        } catch (Exception e) {
            Log.error("Error creating store", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating store").build();
        }
    }

    @PUT
    @Path("/{ id }")
    @Operation(summary = "Update store", description = "Updates an existing store")
    @RequiresStoreRole({}) // Verify store access even for admin operations
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Store updated successfully"),
            @APIResponse(responseCode = "404", description = "Store not found"),
            @APIResponse(responseCode = "409", description = "Subdomain already exists")
    })
    public Response updateStore(
            @PathParam("id") UUID id,
            @Valid StoreDto storeDto) {

        try {
            Store store = storeService.findById(id)
                    .orElseThrow(() -> new NotFoundException("Store not found with ID: " + id));

            // Update fields (except storeKey and schemaName which cannot be changed)
            store.setName(storeDto.name());
            store.setStatus(storeDto.status());
            store.setBillingPlan(storeDto.billingPlan());
            store.setSettings(storeDto.settings());

            // Check if trying to update subdomain and if it's already taken
            if (!store.getSubdomain().equals(storeDto.subdomain())) {
                if (storeService.findBySubdomain(storeDto.subdomain()).isPresent()) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(Map.of("error", "Subdomain already exists"))
                            .build();
                }
                store.setSubdomain(storeDto.subdomain());
            }

            store = storeService.updateStore(store);
            return Response.ok(convertToDto(store)).build();

        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            Log.error("Error updating store", e);
            return Response.serverError()
                    .entity(Map.of("error", "Failed to update store: " + e.getMessage()))
                    .build();
        }
    }

    @PATCH
    @Path("/{ id }/status")
    @Operation(summary = "Update store status", description = "Updates a store's status")
    @RequiresStoreRole({}) // Verify store access even for admin operations
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Store status updated successfully"),
            @APIResponse(responseCode = "400", description = "Invalid status"),
            @APIResponse(responseCode = "404", description = "Store not found")
    })
    public Response updateStoreStatus(
            @PathParam("id") UUID id,
            Map<String, String> body) {

        String status = body.get("status");
        if (status == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Status is required"))
                    .build();
        }

        try {
            StoreStatus storeStatus = StoreStatus.valueOf(status.toUpperCase());
            Store store = storeService.updateStoreStatus(id, storeStatus);
            return Response.ok(convertToDto(store)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid status value: " + status))
                    .build();
        } catch (Exception e) {
            Log.error("Error updating store status", e);
            return Response.serverError()
                    .entity(Map.of("error", "Failed to update store status: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{ id }")
    @Operation(summary = "Delete store", description = "Soft deletes a store")
    @RequiresStoreRole({}) // Verify store access even for admin operations
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Store deleted successfully"),
            @APIResponse(responseCode = "404", description = "Store not found")
    })
    public Response deleteStore(@PathParam("id") UUID id) {
        try {
            storeService.deleteStore(id);
            return Response.noContent().build();
        } catch (Exception e) {
            Log.error("Error deleting store", e);
            return Response.serverError()
                    .entity(Map.of("error", "Failed to delete store: " + e.getMessage()))
                    .build();
        }
    }

    private StoreDto convertToDto(Store store) {
        return new StoreDto(
                store.getId(),
                store.getStoreKey(),
                store.getName(),
                store.getSubdomain(),
                store.getStatus(),
                store.getBillingPlan(),
                store.getSettings());
    }
}
