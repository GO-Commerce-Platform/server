package dev.tiodati.saas.gocommerce.resource;

import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.tiodati.saas.gocommerce.service.TenantSettingsService;
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
 * REST endpoint for managing tenant settings.
 */
@Path("/api/admin/tenants/{tenantId}/settings")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Tenant Settings", description = "Operations for managing tenant settings")
public class TenantSettingsResource {

    private final TenantSettingsService settingsService;
    
    @Inject
    public TenantSettingsResource(TenantSettingsService settingsService) {
        this.settingsService = settingsService;
    }
    
    /**
     * Get a tenant setting by path
     * 
     * @param tenantId The tenant ID
     * @param path The setting path (e.g., "theme.primaryColor")
     * @param defaultValue Optional default value if setting is not found
     * @return The setting value or the default
     */
    @GET
    @Path("/get")
    @Operation(summary = "Get a tenant setting", description = "Retrieves a tenant setting by path")
    @RolesAllowed({"admin", "tenant_admin"})
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Setting retrieved successfully"),
        @APIResponse(responseCode = "404", description = "Tenant not found")
    })
    public Response getSetting(
            @PathParam("tenantId") UUID tenantId,
            @QueryParam("path") String path,
            @QueryParam("default") String defaultValue) {
        
        if (path == null || path.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Path parameter is required"))
                    .build();
        }
        
        String value = settingsService.getSetting(tenantId, path, defaultValue != null ? defaultValue : "");
        return Response.ok(Map.of("value", value)).build();
    }
    
    /**
     * Update a tenant setting
     * 
     * @param tenantId The tenant ID
     * @param body Map containing path and value properties
     * @return Success or failure response
     */
    @POST
    @Path("/update")
    @Operation(summary = "Update a tenant setting", description = "Updates a single tenant setting")
    @RolesAllowed({"admin", "tenant_admin"})
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Setting updated successfully"),
        @APIResponse(responseCode = "400", description = "Invalid input"),
        @APIResponse(responseCode = "404", description = "Tenant not found")
    })
    public Response updateSetting(
            @PathParam("tenantId") UUID tenantId,
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
        
        boolean success = settingsService.updateSetting(tenantId, path, value);
        
        if (!success) {
            throw new NotFoundException("Tenant not found with ID: " + tenantId);
        }
        
        return Response.ok(Map.of("success", true)).build();
    }
    
    /**
     * Bulk update tenant settings
     * 
     * @param tenantId The tenant ID
     * @param settings Map of setting paths to values
     * @return Success or failure response
     */
    @POST
    @Path("/bulk-update")
    @Operation(summary = "Bulk update tenant settings", description = "Updates multiple tenant settings at once")
    @RolesAllowed({"admin", "tenant_admin"})
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Settings updated successfully"),
        @APIResponse(responseCode = "404", description = "Tenant not found"),
        @APIResponse(responseCode = "400", description = "Invalid input")
    })
    public Response bulkUpdateSettings(
            @PathParam("tenantId") UUID tenantId,
            Map<String, String> settings) {
        
        if (settings == null || settings.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Settings map cannot be empty"))
                    .build();
        }
        
        boolean success = settingsService.updateSettings(tenantId, settings);
        
        if (!success) {
            throw new NotFoundException("Tenant not found with ID: " + tenantId);
        }
        
        return Response.ok(Map.of("success", true)).build();
    }
}