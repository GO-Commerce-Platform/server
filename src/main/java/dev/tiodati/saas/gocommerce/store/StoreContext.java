package dev.tiodati.saas.gocommerce.store;

import jakarta.enterprise.context.RequestScoped;
import io.quarkus.logging.Log;

/**
 * Manages the store context for the current request or operation.
 * This class allows explicit store context switching when needed.
 */
@RequestScoped
public class StoreContext {

    private static final ThreadLocal<String> CURRENT_STORE = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_STORE_ID = new ThreadLocal<>();
    
    /**
     * Sets the current store ID for the executing thread.
     * 
     * @param storeId The store identifier (schema name)
     */
    public static void setCurrentStore(String storeId) {
        CURRENT_STORE.set(storeId);
    }
    
    /**
     * Gets the current store ID for the executing thread.
     * 
     * @return The current store identifier or null if not set
     */
    public static String getCurrentStore() {
        return CURRENT_STORE.get();
    }
    
    /**
     * Clears the current store ID from the executing thread.
     * This should be called at the end of operations to prevent memory leaks.
     */
    public static void clear() {
        Log.debug("Clearing store context");
        CURRENT_SCHEMA.remove();
        CURRENT_STORE_ID.remove();
        CURRENT_STORE.remove();
    }
    
    /**
     * Get the current schema from the context.
     * 
     * @return The current schema name or null if not set
     */
    public static String getCurrentSchema() {
        return CURRENT_SCHEMA.get();
    }
    
    /**
     * Set the current schema in the context.
     * 
     * @param schema The schema name to set
     */
    public static void setCurrentSchema(String schema) {
        Log.debugf("Setting current schema to: %s", schema);
        CURRENT_SCHEMA.set(schema);
    }
    
    /**
     * Get the current store ID from the context.
     * 
     * @return The current store ID or null if not set
     */
    public static String getCurrentStoreId() {
        return CURRENT_STORE_ID.get();
    }
    
    /**
     * Set the current store ID in the context.
     * 
     * @param storeId The store ID to set
     */
    public static void setCurrentStoreId(String storeId) {
        Log.debugf("Setting current store ID to: %s", storeId);
        CURRENT_STORE_ID.set(storeId);
    }
}