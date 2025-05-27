package dev.tiodati.saas.gocommerce.store.config;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.PersistenceUnitExtension; // Essential annotation
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
// Import CurrentVertxRequest if you resolve tenant from HTTP request for non-default cases
// import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

@ApplicationScoped
@Unremovable
@PersistenceUnitExtension // Ensure this annotation is present for the default
                          // PU
public class StoreSchemaResolver implements TenantResolver {

    /**
     * The default tenant schema name, which is configured in application
     * properties. This will be used during tests to ensure the correct schema
     * is used.
     */
    @Inject
    @ConfigProperty(name = "quarkus.hibernate-orm.database.default-schema")
    private String defaultTenantSchema;

    @Override
    public String getDefaultTenantId() {
        // This will now correctly return "gocommerce_test" during tests
        return defaultTenantSchema;
    }

    @Override
    public String resolveTenantId() {
        // Implement your logic to resolve tenant ID from request (e.g.,
        // subdomain)
        // For example:
        // RoutingContext context = currentVertxRequest.getCurrent();
        // if (context != null) {
        // String subdomain = context.request().host().split("\\.")[0];
        // if (!"www".equals(subdomain) && !"localhost".equals(subdomain) &&
        // !subdomain.isEmpty()) {
        // // Potentially map subdomain to a schema name, e.g., "store_" +
        // subdomain
        // return "store_" + subdomain;
        // }
        // }
        // Fallback to default tenant ID if no specific tenant is resolved
        return getDefaultTenantId();
    }
}
