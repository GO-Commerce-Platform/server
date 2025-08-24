package dev.tiodati.saas.gocommerce.store.config;

import dev.tiodati.saas.gocommerce.store.StoreContext;
import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.PersistenceUnitExtension; // Essential annotation
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

// @ApplicationScoped - DISABLED: Now using UnifiedTenantResolver
// @Unremovable
// @PersistenceUnitExtension
public class StoreSchemaResolver implements TenantResolver {

    /**
     * The default tenant schema name, which is configured in application
     * properties. This will be used when no specific store context is set.
     */
    @Inject
    @ConfigProperty(name = "quarkus.hibernate-orm.database.default-schema")
    private String defaultTenantSchema;

    @PostConstruct
    public void init() {
        Log.infof("RESOLVER: StoreSchemaResolver initialized! Default schema: %s", defaultTenantSchema);
    }

    @Override
    public String getDefaultTenantId() {
        return defaultTenantSchema;
    }

    @Override
    public String resolveTenantId() {
        // First check if there's a current store context set
        String currentStore = StoreContext.getCurrentStore();
        Log.infof("RESOLVER: resolveTenantId called - StoreContext.getCurrentStore() = %s", currentStore);
        if (currentStore != null && !currentStore.trim().isEmpty()) {
            Log.infof("RESOLVER: Resolved tenant from StoreContext: %s", currentStore);
            return currentStore;
        }
        
        // If no store context is set, use the default tenant ID
        Log.infof("RESOLVER: No store context set, using default tenant: %s", defaultTenantSchema);
        return getDefaultTenantId();
    }
}
