package dev.tiodati.saas.gocommerce.store;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;

/**
 * Example of a path-based store resolver.
 * Currently DISABLED to avoid conflicts with StoreSchemaResolver.
 * To enable, uncomment the @PersistenceUnitExtension and @ApplicationScoped
 * annotations.
 */
// @PersistenceUnitExtension
// @ApplicationScoped
public class RequestPathStoreResolver implements TenantResolver {

    // Inject RoutingContext to access request details
    @Inject
    RoutingContext routingContext;

    // Optional: Inject a default store ID from application.properties
    @ConfigProperty(name = "quarkus.hibernate-orm.database.default-store", defaultValue = "public")
    String defaultStoreId;

    @Override
    public String resolveTenantId() {
        // Example: Try to get store ID from path parameter named 'storeId'
        // Adjust "storeId" to match your actual path parameter name
        String storeId = routingContext.pathParam("storeId");

        if (storeId != null && !storeId.trim().isEmpty()) {
            // You might want to validate the storeId against a list of known stores here
            return storeId;
        }

        // Fallback logic if storeId is not found in the path
        // Option 1: Use a default store ID
        // return defaultStoreId;

        // Option 2: Throw an exception if store cannot be resolved
        // throw new IllegalStateException("Store ID could not be resolved from the
        // request path.");

        // Option 3: Check other sources like headers or JWT token
        // String headerStore = routingContext.request().getHeader("X-Store-ID");
        // if (headerStore != null) return headerStore;

        // For now, let's return the default store if not found in path
        return defaultStoreId;
    }

    @Override
    public String getDefaultTenantId() {
        return defaultStoreId;
    }
}
