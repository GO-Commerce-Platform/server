package dev.tiodati.saas.gocommerce.store.resource;

import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.tiodati.saas.gocommerce.auth.annotation.RequiresStoreRole;
import dev.tiodati.saas.gocommerce.auth.model.Roles;
import dev.tiodati.saas.gocommerce.store.service.StoreSettingsService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST endpoint for managing store settings.
 */
@Path("/api/v1/admin/stores/{ storeId }/settings")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Store Settings", description = "Operations for managing store settings")
public class StoreSettingsResource {

    /**
     * Service for managing store settings.
     */
    private final StoreSettingsService settingsService;

    @Inject
    public StoreSettingsResource(StoreSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Get a store setting by path.
     *
     * @param storeId      The store ID
     * @param path         The setting path (e.g., "theme.primaryColor")
     * @param defaultValue Optional default value if setting is not found
     * @return The setting value or the defaul
     */
    @GET
    @Path("/get")
    @Operation(summary = "Get a store setting", description = "Retrieves a store setting by path")
    @RolesAllowed({ "PLATFORM_ADMIN", "STORE_ADMIN" })
    @RequiresStoreRole({ Roles.STORE_ADMIN })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Setting retrieved successfully"),
            @APIResponse(responseCode = "404", description = "Store not found")
    })
    public Response getSetting(
            @PathParam("storeId") UUID storeId,
            @QueryParam("path") String path,
            @QueryParam("default") String defaultValue) {

        if (path == null || path.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Path parameter is required"))
                    .build();
        }

        String value = settingsService.getSetting(storeId, path, defaultValue != null ? defaultValue : "");
        return Response.ok(Map.of("value", value)).build();
    }

    /**
     * Update a store setting.
     *
     * @param storeId The store ID
     * @param body    Map containing path and value properties
     * @return Success or failure response
     */
    @POST
    @Path("/update")
    @Operation(summary = "Update a store setting", description = "Updates a single store setting")
    @RolesAllowed({ "PLATFORM_ADMIN", "STORE_ADMIN" })
    @RequiresStoreRole({ Roles.STORE_ADMIN })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Setting updated successfully"),
            @APIResponse(responseCode = "400", description = "Invalid input"),
            @APIResponse(responseCode = "404", description = "Store not found")
    })
    public Response updateSetting(
            @PathParam("storeId") UUID storeId,
            Map<String, String> body) {

        String path = body.get("path");
        String value = body.get("value");

        if (path == null || path.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Path parameter is required"))
                    .build();
        }

        if (value == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Value parameter is required"))
                    .build();
        }

        boolean success = settingsService.updateSetting(storeId, path, value);

        if (!success) {
            throw new NotFoundException("Store not found with ID: " + storeId);
        }

        return Response.ok(Map.of("success", true)).build();
    }

    /**
     * Bulk update store settings.
     *
     * @param storeId  The store ID
     * @param settings Map of setting paths to values
     * @return Success or failure response
     */
    @POST
    @Path("/bulk-update")
    @Operation(summary = "Bulk update store settings", description = "Updates multiple store settings at once")
    @RolesAllowed({ "PLATFORM_ADMIN", "STORE_ADMIN" })
    @RequiresStoreRole({ Roles.STORE_ADMIN })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Settings updated successfully"),
            @APIResponse(responseCode = "404", description = "Store not found"),
            @APIResponse(responseCode = "400", description = "Invalid input")
    })
    public Response bulkUpdateSettings(
            @PathParam("storeId") UUID storeId,
            Map<String, String> settings) {

        if (settings == null || settings.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Settings map cannot be empty"))
                    .build();
        }

        boolean success = settingsService.updateSettings(storeId, settings);

        if (!success) {
            throw new NotFoundException("Store not found with ID: " + storeId);
        }

        return Response.ok(Map.of("success", true)).build();
    }
}
