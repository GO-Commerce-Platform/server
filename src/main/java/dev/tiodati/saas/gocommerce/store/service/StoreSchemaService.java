package dev.tiodati.saas.gocommerce.store.service;

import dev.tiodati.saas.gocommerce.store.entity.Store;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service for resolving store schemas from store IDs.
 * This service maintains a cache of store ID to schema mappings
 * and provides the schema information needed for multi-tenant operations.
 */
@ApplicationScoped
public class StoreSchemaService {

    private final Map<UUID, String> storeSchemaCache = new ConcurrentHashMap<>();

    public StoreSchemaService() {
    }

    /**
     * Resolves the schema name for a given store ID.
     * 
     * @param storeId The store ID
     * @return The schema name for the store, or null if not found
     */
    @Transactional
    public String resolveStoreSchema(UUID storeId) {
        // Check cache first
        String cachedSchema = storeSchemaCache.get(storeId);
        if (cachedSchema != null) {
            return cachedSchema;
        }

        // Query store from database using master schema (gocommerce)
        Store store = Store.findById(storeId);
        if (store != null) {
            String schema = store.getSchemaName();
            // Cache the result
            storeSchemaCache.put(storeId, schema);
            Log.debugf("Resolved store %s to schema %s", storeId, schema);
            return schema;
        }

        Log.warnf("Could not resolve schema for store ID: %s", storeId);
        return null;
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