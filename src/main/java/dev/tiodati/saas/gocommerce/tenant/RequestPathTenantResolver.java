package dev.tiodati.saas.gocommerce.tenant;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;

/**
 * Example of a path-based tenant resolver.
 * Currently DISABLED to avoid conflicts with TenantSchemaResolver.
 * To enable, uncomment the @PersistenceUnitExtension and @ApplicationScoped annotations.
 */
// @PersistenceUnitExtension 
// @ApplicationScoped
public class RequestPathTenantResolver implements TenantResolver {

    // Inject RoutingContext to access request details
    @Inject
    RoutingContext routingContext;

    // Optional: Inject a default tenant ID from application.properties
    @ConfigProperty(name = "quarkus.hibernate-orm.database.default-tenant", defaultValue = "public")
    String defaultTenantId;

    @Override
    public String resolveTenantId() {
        // Example: Try to get tenant ID from path parameter named 'tenantId'
        // Adjust "tenantId" to match your actual path parameter name
        String tenantId = routingContext.pathParam("tenantId");

        if (tenantId != null && !tenantId.trim().isEmpty()) {
            // You might want to validate the tenantId against a list of known tenants here
            return tenantId;
        }

        // Fallback logic if tenantId is not found in the path
        // Option 1: Use a default tenant ID
        // return defaultTenantId;

        // Option 2: Throw an exception if tenant cannot be resolved
        // throw new IllegalStateException("Tenant ID could not be resolved from the request path.");

        // Option 3: Check other sources like headers or JWT token
        // String headerTenant = routingContext.request().getHeader("X-Tenant-ID");
        // if (headerTenant != null) return headerTenant;

        // For now, let's return the default tenant if not found in path
        return defaultTenantId;
    }
    
    @Override
    public String getDefaultTenantId() {
        return defaultTenantId;
    }
}
