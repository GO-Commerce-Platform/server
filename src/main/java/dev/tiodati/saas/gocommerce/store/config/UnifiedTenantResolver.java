package dev.tiodati.saas.gocommerce.store.config;

import dev.tiodati.saas.gocommerce.store.StoreContext;
import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.logging.Log;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Unified tenant resolver that works in both production and test environments.
 * In production, it uses StoreContext. In tests, it prioritizes TestTenantContext and HTTP headers.
 */
@ApplicationScoped
@Unremovable
@PersistenceUnitExtension
public class UnifiedTenantResolver implements TenantResolver {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    @Inject
    @ConfigProperty(name = "quarkus.hibernate-orm.database.default-schema")
    private String defaultTenantSchema;

    @Inject
    Instance<RoutingContext> routingContextInstance;

    @PostConstruct
    public void init() {
        Log.infof("🚀 UNIFIED RESOLVER: UnifiedTenantResolver initialized! Default schema: %s", defaultTenantSchema);
        Log.infof("📋 UNIFIED: Routing context instance available: %s", routingContextInstance != null ? "YES" : "NO");
    }

    @Override
    public String getDefaultTenantId() {
        return defaultTenantSchema;
    }

    @Override
    public String resolveTenantId() {
        Log.infof("🏢 UNIFIED RESOLVER: *** RESOLVING TENANT *** resolveTenantId() called");
        
        // Strategy 1: Check for TestTenantContext (test environment)
        Log.infof("🇺 UNIFIED: Attempting to resolve from TestTenantContext...");
        String testTenant = resolveFromTestContext();
        if (testTenant != null && !testTenant.trim().isEmpty()) {
            Log.infof("✅ UNIFIED: Resolved tenant from test context: %s", testTenant);
            return testTenant;
        } else {
            Log.infof("📋 UNIFIED: No tenant found in TestTenantContext");
        }

        // Strategy 2: Check HTTP header (test environment with REST calls)
        Log.infof("🌐 UNIFIED: Attempting to resolve from HTTP headers...");
        String headerTenant = resolveFromHttpHeader();
        if (headerTenant != null && !headerTenant.trim().isEmpty()) {
            Log.infof("✅ UNIFIED: Resolved tenant from HTTP header: %s", headerTenant);
            return headerTenant;
        } else {
            Log.infof("📋 UNIFIED: No tenant found in HTTP headers");
        }

        // Strategy 3: Check StoreContext (production environment)
        Log.infof("🏪 UNIFIED: Attempting to resolve from StoreContext...");
        String storeContextTenant = resolveFromStoreContext();
        if (storeContextTenant != null && !storeContextTenant.trim().isEmpty()) {
            Log.infof("✅ UNIFIED: Resolved tenant from StoreContext: %s", storeContextTenant);
            return storeContextTenant;
        } else {
            Log.infof("📋 UNIFIED: No tenant found in StoreContext");
        }

        // Fallback: Use default tenant
        Log.infof("🔄 UNIFIED: No tenant context found, using default: %s", defaultTenantSchema);
        Log.infof("🎯 UNIFIED: *** FINAL RESULT *** returning tenant: %s", defaultTenantSchema);
        return getDefaultTenantId();
    }

    /**
     * Resolve tenant from TestTenantContext (test environment)
     */
    private String resolveFromTestContext() {
        try {
            Log.infof("🇺 UNIFIED: Checking Arc container...");
            if (Arc.container() != null) {
                Log.infof("🇺 UNIFIED: Arc container found, looking for TestTenantContext...");
                var testTenantContextInstance = Arc.container().select(
                    Class.forName("dev.tiodati.saas.gocommerce.testinfra.TestTenantContext")
                );
                if (testTenantContextInstance.isResolvable()) {
                    Log.infof("🇺 UNIFIED: TestTenantContext found and resolvable!");
                    Object testTenantContext = testTenantContextInstance.get();
                    // Use reflection to call getCurrentTenant()
                    var method = testTenantContext.getClass().getMethod("getCurrentTenant");
                    String tenant = (String) method.invoke(testTenantContext);
                    Log.infof("🇺 UNIFIED: TestTenantContext returned: '%s'", tenant);
                    if (tenant != null && !tenant.trim().isEmpty()) {
                        return tenant;
                    }
                } else {
                    Log.infof("🇺 UNIFIED: TestTenantContext found but not resolvable");
                }
            } else {
                Log.infof("🇺 UNIFIED: Arc container is null");
            }
        } catch (Exception e) {
            // TestTenantContext not available (likely production environment)
            Log.infof("⚠️ UNIFIED: TestTenantContext not available: %s", e.getMessage());
        }
        return null;
    }

    /**
     * Resolve tenant from HTTP header (test environment)
     */
    private String resolveFromHttpHeader() {
        try {
            if (routingContextInstance != null && routingContextInstance.isResolvable()) {
                RoutingContext routingContext = routingContextInstance.get();
                if (routingContext != null) {
                    return routingContext.request().getHeader(TENANT_HEADER);
                }
            }
        } catch (Exception e) {
            Log.debugf("UNIFIED: HTTP context not available: %s", e.getMessage());
        }
        return null;
    }

    /**
     * Resolve tenant from StoreContext (production environment)
     */
    private String resolveFromStoreContext() {
        try {
            return StoreContext.getCurrentStore();
        } catch (Exception e) {
            Log.debugf("UNIFIED: StoreContext not available: %s", e.getMessage());
        }
        return null;
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
