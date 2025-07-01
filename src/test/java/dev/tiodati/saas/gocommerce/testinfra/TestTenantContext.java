package dev.tiodati.saas.gocommerce.testinfra;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A context holder for setting the tenant ID during tests.
 * This is used as a fallback by the TestTenantResolver when not in an HTTP request context.
 */
@ApplicationScoped
public class TestTenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }

    public String getCurrentTenant() {
        return currentTenant.get();
    }

    public void clear() {
        currentTenant.remove();
    }
}
