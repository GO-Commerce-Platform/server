package dev.tiodati.saas.gocommerce.store.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.tiodati.saas.gocommerce.store.StoreContext;
import dev.tiodati.saas.gocommerce.store.entity.Store;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Service for resolving store schemas from store IDs.
 * This service maintains a cache of store ID to schema mappings
 * and provides the schema information needed for multi-tenant operations.
 */
@ApplicationScoped
public class StoreSchemaService {

    private final Map<UUID, String> storeSchemaCache = new ConcurrentHashMap<>();
    private final EntityManager em;

    @Inject
    public StoreSchemaService(EntityManager em) {
        this.em = em;
    }

    /**
     * Resolves the schema name for a given store ID.
     *
     * @param storeId The store ID
     * @return The schema name for the store, or null if not found
     */
    public String resolveStoreSchema(UUID storeId) {
        // Check cache first
        String cachedSchema = storeSchemaCache.get(storeId);
        if (cachedSchema != null) {
            return cachedSchema;
        }

        // Use a new transaction to ensure the store lookup is isolated and successful,
        // especially in test environments where the store might have been created in a separate,
        // recently committed transaction.
        return QuarkusTransaction.requiringNew().call(() -> {
            Store store = em.find(Store.class, storeId);
            if (store != null) {
                String schema = store.getSchemaName();
                // Cache the result
                storeSchemaCache.put(storeId, schema);
                Log.debugf("Resolved store %s to schema %s", storeId, schema);
                return schema;
            }
            Log.warnf("Could not resolve schema for store ID: %s", storeId);
            return null;
        });
    }

    /**
     * Resolves the schema for a given store ID and sets it as the current tenant context.
     * This should be called at the beginning of any operation that needs to be
     * performed within a specific store's schema.
     *
     * @param storeId The store ID
     */
    public void setStoreSchema(UUID storeId) {
        String schemaName = resolveStoreSchema(storeId);
        if (schemaName != null) {
            StoreContext.setCurrentStore(schemaName);
            Log.infof("Set tenant context for store %s to schema %s", storeId, schemaName);
        } else {
            // It's important to clear the context if a schema can't be resolved
            // to prevent using a stale context from a previous operation.
            StoreContext.clear();
            Log.warnf("Could not set tenant context for store ID: %s. Context cleared.", storeId);
        }
    }

    /**
     * Clears the cache for a specific store ID.
     *
     * @param storeId The store ID to remove from cache
     */
    public void clearStoreSchemaCache(UUID storeId) {
        storeSchemaCache.remove(storeId);
    }

    /**
     * Clears all cached store schema mappings.
     */
    public void clearAllStoreSchemaCache() {
        storeSchemaCache.clear();
    }
}
