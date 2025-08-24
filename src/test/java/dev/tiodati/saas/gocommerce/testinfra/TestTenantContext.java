package dev.tiodati.saas.gocommerce.testinfra;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A context holder for setting the tenant ID during tests.
 * This is used as a fallback by the TestTenantResolver when not in an HTTP request context.
 */
@ApplicationScoped
public class TestTenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    @PostConstruct
    public void init() {
        Log.infof("🎉 TestTenantContext initialized - ready to track tenant changes!");
    }

    public void setCurrentTenant(String tenantId) {
        String previousTenant = currentTenant.get();
        Log.infof("🎯 TestTenantContext: *** SETTING TENANT *** to: %s (previous: %s, thread: %s)", 
                 tenantId, previousTenant, Thread.currentThread().getName());
        currentTenant.set(tenantId);
    }

    public String getCurrentTenant() {
        String tenant = currentTenant.get();
        Log.infof("🔍 TestTenantContext: *** GETTING TENANT *** returning: %s (thread: %s)", 
                 tenant, Thread.currentThread().getName());
        return tenant;
    }

    public void clear() {
        String tenant = currentTenant.get();
        Log.infof("🧩 TestTenantContext: *** CLEARING TENANT *** was: %s (thread: %s)", 
                 tenant, Thread.currentThread().getName());
        currentTenant.remove();
    }
}
