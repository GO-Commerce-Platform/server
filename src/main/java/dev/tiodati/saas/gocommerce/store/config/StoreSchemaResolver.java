package dev.tiodati.saas.gocommerce.store.config;

import io.quarkus.logging.Log;

import dev.tiodati.saas.gocommerce.store.StoreContext;
import dev.tiodati.saas.gocommerce.store.model.Store;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@LookupIfProperty(name = "quarkus.hibernate-orm.multistore", stringValue = "SCHEMA")
@RequestScoped
@PersistenceUnitExtension
// Added @PersistenceUnitExtension to fix the warning about future compatibility
public class StoreSchemaResolver implements TenantResolver {
    
    private static final String DEFAULT_STORE_ID = "default";
    
    @Inject
    CurrentVertxRequest currentVertxRequest;
    
    @Inject
    EntityManager em;
    
    @Override
    public String getDefaultTenantId() {
        return DEFAULT_STORE_ID;
    }
    
    @Override
    public String resolveTenantId() {
        // First check if store ID is set in StoreContext
        String storeFromContext = StoreContext.getCurrentStore();
        if (storeFromContext != null && !storeFromContext.isEmpty()) {
            Log.debug("Resolved store from context: " + storeFromContext);
            return storeFromContext;
        }
        
        if (currentVertxRequest == null) {
            Log.debug("No current Vertx request context available, using default store");
            return getDefaultTenantId();
        }
        
        RoutingContext context = currentVertxRequest.getCurrent();
        if (context == null) {
            Log.debug("No routing context available, using default store");
            return getDefaultTenantId();
        }
        
        // Try to get store from header
        String storeKey = context.request().getHeader("X-Store");
        if (storeKey != null && !storeKey.isEmpty()) {
            Log.debug("Resolving store from X-Store header: " + storeKey);
            String schemaName = getSchemaNameFromStoreKey(storeKey);
            if (schemaName != null) {
                // Store in context for this request
                StoreContext.setCurrentStore(schemaName);
                return schemaName;
            }
        }
        
        // If header not available, try to extract from subdomain
        String host = context.request().getHeader("Host");
        if (host != null && host.contains(".")) {
            String subdomain = host.split("\\.")[0];
            if (!"www".equals(subdomain)) {
                Log.debug("Resolving store from subdomain: " + subdomain);
                String schemaName = getSchemaNameFromSubdomain(subdomain);
                if (schemaName != null) {
                    // Store in context for this request
                    StoreContext.setCurrentStore(schemaName);
                    return schemaName;
                }
            }
        }
        
        Log.debug("Could not resolve store, using default");
        return getDefaultTenantId();
    }
    
    /**
     * Gets the schema name for a store based on store key.
     * Changed from private to protected for better testability.
     * 
     * @param storeKey The store key to look up
     * @return The schema name or null if not found
     */
    protected String getSchemaNameFromStoreKey(String storeKey) {
        try {
            Store store = em.createQuery(
                "SELECT t FROM Store t WHERE t.storeKey = :storeKey", Store.class)
                .setParameter("storeKey", storeKey)
                .getSingleResult();
            return store.getSchemaName();
        } catch (Exception e) {
            Log.warn("Error resolving schema from store key: " + storeKey, e);
            return null;
        }
    }
    
    /**
     * Gets the schema name for a store based on subdomain.
     * Changed from private to protected for better testability.
     * 
     * @param subdomain The subdomain to look up
     * @return The schema name or null if not found
     */
    protected String getSchemaNameFromSubdomain(String subdomain) {
        try {
            Store store = em.createQuery(
                "SELECT t FROM Store t WHERE t.subdomain = :subdomain", Store.class)
                .setParameter("subdomain", subdomain)
                .getSingleResult();
            return store.getSchemaName();
        } catch (Exception e) {
            Log.warn("Error resolving schema from subdomain: " + subdomain, e);
            return null;
        }
    }
}