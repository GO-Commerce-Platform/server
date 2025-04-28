package dev.tiodati.saas.gocommerce.tenant;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.hibernate.orm.PersistenceUnitExtension; // Import the annotation
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@PersistenceUnitExtension // Add this annotation
@ApplicationScoped
public class RequestPathTenantResolver implements TenantResolver {

    // Inject RoutingContext to access request details
    @Inject
    RoutingContext routingContext;

    // Optional: Inject a default tenant ID from application.properties
    @ConfigProperty(name = "quarkus.hibernate-orm.database.default-tenant", defaultValue = "public") // Example default
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

    // Optional: Define a default tenant ID in application.properties
    // quarkus.hibernate-orm.database.default-tenant=public
    
    @Override
    public String getDefaultTenantId() {
        return defaultTenantId;
    }
}
