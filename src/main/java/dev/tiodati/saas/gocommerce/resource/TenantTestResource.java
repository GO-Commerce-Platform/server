package dev.tiodati.saas.gocommerce.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.tiodati.saas.gocommerce.tenant.TenantContext;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Resource for testing the multi-tenant functionality.
 */
@Path("/api/test/tenant")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Tenant Testing", description = "Endpoints for testing tenant isolation")
public class TenantTestResource {
    
    @Inject
    @PersistenceUnitExtension
    TenantResolver tenantResolver;
    
    @GET
    @Operation(summary = "Get current tenant", description = "Returns information about the current tenant context")
    public Response getCurrentTenant(@Context UriInfo uriInfo) {
        String resolvedTenantId = tenantResolver.resolveTenantId();
        String contextTenantId = TenantContext.getCurrentTenant();
        String hostHeader = uriInfo.getRequestUri().getHost();
        
        return Response.ok(Map.of(
                "resolvedTenantId", resolvedTenantId,
                "contextTenantId", contextTenantId != null ? contextTenantId : "not set",
                "host", hostHeader,
                "defaultTenantId", tenantResolver.getDefaultTenantId()
        )).build();
    }
}