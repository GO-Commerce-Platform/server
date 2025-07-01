package dev.tiodati.saas.gocommerce.testinfra;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

/**
 * Test-specific tenant resolver that reads the tenant ID from the 'X-Tenant-ID' HTTP header.
 * This allows tests to control the tenant context for each REST call in a reliable way.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class TestTenantResolver implements TenantResolver {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    @Inject
    RoutingContext routingContext;

    @Override
    public String resolveTenantId() {
        if (routingContext != null) {
            String tenantId = routingContext.request().getHeader(TENANT_HEADER);
            if (tenantId != null && !tenantId.trim().isEmpty()) {
                return tenantId;
            }
        }
        
        // Fallback for non-HTTP contexts (e.g., direct service calls in tests)
        return resolveTenantIdFromArc();
    }

    /**
     * Resolves tenant from Arc context if available.
     * This is a fallback for tests that do not run in an HTTP request context.
     */
    private String resolveTenantIdFromArc() {
        try {
            if (Arc.container() != null && Arc.container().instance(TestTenantContext.class).isAvailable()) {
                TestTenantContext testTenantContext = Arc.container().instance(TestTenantContext.class).get();
                if (testTenantContext != null && testTenantContext.getCurrentTenant() != null) {
                    return testTenantContext.getCurrentTenant();
                }
            }
        } catch (Exception e) {
            // Ignore if Arc container or bean is not available
        }
        return null; // No default tenant for tests
    }

    @Override
    public String getDefaultTenantId() {
        return null; // No default tenant for tests
    }
}

