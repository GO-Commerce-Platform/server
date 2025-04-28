package dev.tiodati.saas.gocommerce.resource;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import dev.tiodati.saas.gocommerce.model.tenant.Tenant;
import dev.tiodati.saas.gocommerce.model.tenant.TenantAdmin;
import dev.tiodati.saas.gocommerce.model.tenant.TenantStatus;
import dev.tiodati.saas.gocommerce.resource.dto.CreateTenantDto;
import dev.tiodati.saas.gocommerce.resource.dto.TenantDto;
import dev.tiodati.saas.gocommerce.service.TenantService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
    
    @Inject
    TenantService tenantService;
    
    @GET
    @Operation(summary = "List all tenants", description = "Returns a list of all tenants")
    @RolesAllowed("admin")
    public List<TenantDto> listTenants() {
        return tenantService.listAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @GET
    @Path("/{id}")
    @Operation(summary = "Get tenant by ID", description = "Returns a tenant by its ID")
    @RolesAllowed("admin")
    public Response getTenant(@PathParam("id") Long id) {
        Tenant tenant = tenantService.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found with ID: " + id));
        return Response.ok(convertToDto(tenant)).build();
    }
    
    @POST
    @Transactional
    @Operation(summary = "Create a new tenant", description = "Creates a new tenant with an admin user")
    @RolesAllowed("admin")
    public Response createTenant(@Valid CreateTenantDto createTenantDto) {
        // Check if tenant key or subdomain already exists
        if (tenantService.findByTenantKey(createTenantDto.getTenantKey()).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Tenant key already exists"))
                    .build();
        }
        
        if (tenantService.findBySubdomain(createTenantDto.getSubdomain()).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Subdomain already exists"))
                    .build();
        }
        
        try {
            // Create tenant entity
            Tenant tenant = new Tenant();
            tenant.setTenantKey(createTenantDto.getTenantKey());
            tenant.setName(createTenantDto.getName());
            tenant.setSubdomain(createTenantDto.getSubdomain());
            tenant.setStatus(TenantStatus.TRIAL);
            tenant.setBillingPlan(createTenantDto.getBillingPlan() != null ? 
                    createTenantDto.getBillingPlan() : "BASIC");
            tenant.setSettings(createTenantDto.getSettings());
            
            // Create admin user
            TenantAdmin admin = new TenantAdmin();
            admin.setEmail(createTenantDto.getAdminEmail());
            // In a real application, you'd hash the password
            admin.setPasswordHash(createTenantDto.getAdminPassword());
            admin.setFirstName(createTenantDto.getAdminFirstName());
            admin.setLastName(createTenantDto.getAdminLastName());
            
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
    @Transactional
    @Operation(summary = "Update a tenant", description = "Updates an existing tenant")
    @RolesAllowed("admin")
    public Response updateTenant(@PathParam("id") Long id, TenantDto tenantDto) {
        if (!id.equals(tenantDto.getId())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Path ID does not match payload ID"))
                    .build();
        }
        
        try {
            Tenant tenant = tenantService.findById(id)
                    .orElseThrow(() -> new NotFoundException("Tenant not found with ID: " + id));
            
            // Update fields (except tenantKey and schemaName which cannot be changed)
            tenant.setName(tenantDto.getName());
            tenant.setStatus(tenantDto.getStatus());
            tenant.setBillingPlan(tenantDto.getBillingPlan());
            tenant.setSettings(tenantDto.getSettings());
            
            // Check if trying to update subdomain and if it's already taken
            if (!tenant.getSubdomain().equals(tenantDto.getSubdomain())) {
                if (tenantService.findBySubdomain(tenantDto.getSubdomain()).isPresent()) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(Map.of("error", "Subdomain already exists"))
                            .build();
                }
                tenant.setSubdomain(tenantDto.getSubdomain());
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
    @Transactional
    @Operation(summary = "Update tenant status", description = "Updates the status of an existing tenant")
    @RolesAllowed("admin")
    public Response updateTenantStatus(@PathParam("id") Long id, Map<String, String> statusUpdate) {
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