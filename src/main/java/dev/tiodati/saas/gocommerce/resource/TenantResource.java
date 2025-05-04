package dev.tiodati.saas.gocommerce.resource;

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
import org.jboss.logging.Logger;

import dev.tiodati.saas.gocommerce.model.tenant.Tenant;
import dev.tiodati.saas.gocommerce.model.tenant.TenantAdmin;
import dev.tiodati.saas.gocommerce.model.tenant.TenantStatus;
import dev.tiodati.saas.gocommerce.resource.dto.CreateTenantDto;
import dev.tiodati.saas.gocommerce.resource.dto.TenantDto;
import dev.tiodati.saas.gocommerce.service.TenantService;
import dev.tiodati.saas.gocommerce.util.PasswordHashingUtil;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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

@Path("/api/admin/tenants")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Tenant Management", description = "Operations for managing tenants")
public class TenantResource {

    private static final Logger LOG = Logger.getLogger(TenantResource.class);
    
    private final TenantService tenantService;
    private final PasswordHashingUtil passwordHashingUtil;
    
    @Inject
    public TenantResource(TenantService tenantService, PasswordHashingUtil passwordHashingUtil) {
        this.tenantService = tenantService;
        this.passwordHashingUtil = passwordHashingUtil;
    }
    
    @GET
    @Operation(summary = "List all tenants", description = "Returns a list of all tenants")
    @RolesAllowed("admin")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of tenants retrieved successfully", 
                     content = @Content(schema = @Schema(implementation = TenantDto.class)))
    })
    public List<TenantDto> listTenants() {
        return tenantService.listAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @GET
    @Path("/{id}")
    @Operation(summary = "Get tenant by ID", description = "Returns a tenant by its ID")
    @RolesAllowed("admin")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Tenant retrieved successfully", 
                     content = @Content(schema = @Schema(implementation = TenantDto.class))),
        @APIResponse(responseCode = "404", description = "Tenant not found")
    })
    public Response getTenant(@PathParam("id") UUID id) {
        Tenant tenant = tenantService.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found with ID: " + id));
        return Response.ok(convertToDto(tenant)).build();
    }
    
    @POST
    @Operation(summary = "Create a new tenant", description = "Creates a new tenant with an admin user")
    @RolesAllowed("admin")
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Tenant created successfully"),
        @APIResponse(responseCode = "409", description = "Tenant key or subdomain already exists"),
        @APIResponse(responseCode = "500", description = "Server error")
    })
    public Response createTenant(@Valid CreateTenantDto createTenantDto) {
        // Check if tenant key or subdomain already exists
        if (tenantService.findByTenantKey(createTenantDto.tenantKey()).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Tenant key already exists"))
                    .build();
        }
        
        if (tenantService.findBySubdomain(createTenantDto.subdomain()).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Subdomain already exists"))
                    .build();
        }
        
        try {
            // Create tenant entity
            Tenant tenant = new Tenant();
            tenant.setTenantKey(createTenantDto.tenantKey());
            tenant.setName(createTenantDto.name());
            tenant.setSubdomain(createTenantDto.subdomain());
            tenant.setStatus(TenantStatus.TRIAL);
            tenant.setBillingPlan(createTenantDto.billingPlan() != null ? 
                    createTenantDto.billingPlan() : "BASIC");
            tenant.setSettings(createTenantDto.settings());
            
            // Create admin user
            TenantAdmin admin = new TenantAdmin();
            admin.setEmail(createTenantDto.adminEmail());
            // Hash the password before storing
            String hashedPassword = passwordHashingUtil.hashPassword(createTenantDto.adminPassword());
            admin.setPasswordHash(hashedPassword);
            admin.setFirstName(createTenantDto.adminFirstName());
            admin.setLastName(createTenantDto.adminLastName());
            
            // Create tenant and admin
            Tenant createdTenant = tenantService.createTenant(tenant, admin);
            
            return Response
                    .created(URI.create("/api/admin/tenants/" + createdTenant.getId()))
                    .entity(convertToDto(createdTenant))
                    .build();
                    
        } catch (Exception e) {
            LOG.error("Error creating tenant", e);
            return Response.serverError()
                    .entity(Map.of("error", "Failed to create tenant: " + e.getMessage()))
                    .build();
        }
    }
    
    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a tenant", description = "Updates an existing tenant")
    @RolesAllowed("admin")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Tenant updated successfully"),
        @APIResponse(responseCode = "404", description = "Tenant not found"),
        @APIResponse(responseCode = "409", description = "Subdomain already exists"),
        @APIResponse(responseCode = "500", description = "Server error")
    })
    public Response updateTenant(@PathParam("id") UUID id, TenantDto tenantDto) {
        if (!id.equals(tenantDto.id())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Path ID does not match payload ID"))
                    .build();
        }
        
        try {
            Tenant tenant = tenantService.findById(id)
                    .orElseThrow(() -> new NotFoundException("Tenant not found with ID: " + id));
            
            // Update fields (except tenantKey and schemaName which cannot be changed)
            tenant.setName(tenantDto.name());
            tenant.setStatus(tenantDto.status());
            tenant.setBillingPlan(tenantDto.billingPlan());
            tenant.setSettings(tenantDto.settings());
            
            // Check if trying to update subdomain and if it's already taken
            if (!tenant.getSubdomain().equals(tenantDto.subdomain())) {
                if (tenantService.findBySubdomain(tenantDto.subdomain()).isPresent()) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(Map.of("error", "Subdomain already exists"))
                            .build();
                }
                tenant.setSubdomain(tenantDto.subdomain());
            }
            
            Tenant updatedTenant = tenantService.updateTenant(tenant);
            return Response.ok(convertToDto(updatedTenant)).build();
            
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error updating tenant", e);
            return Response.serverError()
                    .entity(Map.of("error", "Failed to update tenant: " + e.getMessage()))
                    .build();
        }
    }
    
    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Update tenant status", description = "Updates the status of an existing tenant")
    @RolesAllowed("admin")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Tenant status updated successfully"),
        @APIResponse(responseCode = "404", description = "Tenant not found"),
        @APIResponse(responseCode = "400", description = "Invalid status value"),
        @APIResponse(responseCode = "500", description = "Server error")
    })
    public Response updateTenantStatus(@PathParam("id") UUID id, Map<String, String> statusUpdate) {
        if (!statusUpdate.containsKey("status")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Status field is required"))
                    .build();
        }
        
        try {
            TenantStatus status = TenantStatus.valueOf(statusUpdate.get("status"));
            Tenant updatedTenant = tenantService.updateTenantStatus(id, status);
            return Response.ok(convertToDto(updatedTenant)).build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Tenant not found")) {
                throw new NotFoundException(e.getMessage());
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid status value"))
                    .build();
        } catch (Exception e) {
            LOG.error("Error updating tenant status", e);
            return Response.serverError()
                    .entity(Map.of("error", "Failed to update tenant status: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Convert Tenant entity to TenantDto
     */
    private TenantDto convertToDto(Tenant tenant) {
        return new TenantDto(
                tenant.getId(),
                tenant.getTenantKey(),
                tenant.getName(),
                tenant.getSubdomain(),
                tenant.getStatus(),
                tenant.getBillingPlan(),
                tenant.getSettings()
        );
    }
}