package dev.tiodati.saas.gocommerce.store;

import jakarta.enterprise.context.RequestScoped;
import io.quarkus.logging.Log;

/**
 * Manages the store context for the current request or operation using
 * ThreadLocal variables. This class allows explicit store context switching
 * when needed, ensuring that operations are performed against the correct
 * store's data, particularly in a multi-tenant environment.
 *
 * The context includes the store identifier (often the schema name), the actual
 * schema name, and the store's unique ID.
 *
 * It's crucial to clear the context using {@link #clear()} after the request or
 * operation is complete to prevent data leakage between threads or requests and
 * to avoid memory leaks in thread-pooled environments.
 */
@RequestScoped
public class StoreContext {

    /**
     * ThreadLocal holding the current store identifier, often used as the
     * schema name. This was the original field for storing the current store
     * context.
     */
    private static final ThreadLocal<String> CURRENT_STORE = new ThreadLocal<>();
    /**
     * ThreadLocal holding the specific schema name for the current store
     * context.
     */
    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();
    /**
     * ThreadLocal holding the unique ID (e.g., UUID) of the current store.
     */
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
     * @return The current store identifier or null if not se
     */
    public static String getCurrentStore() {
        return CURRENT_STORE.get();
    }

    /**
     * Clears the current store ID from the executing thread. This should be
     * called at the end of operations to prevent memory leaks.
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
     * @return The current schema name or null if not se
     */
    public static String getCurrentSchema() {
        return CURRENT_SCHEMA.get();
    }

    /**
     * Set the current schema in the context.
     *
     * @param schema The schema name to se
     */
    public static void setCurrentSchema(String schema) {
        Log.debugf("Setting current schema to: %s", schema);
        CURRENT_SCHEMA.set(schema);
    }

    /**
     * Get the current store ID from the context.
     *
     * @return The current store ID or null if not se
     */
    public static String getCurrentStoreId() {
        return CURRENT_STORE_ID.get();
    }

    /**
     * Set the current store ID in the context.
     *
     * @param storeId The store ID to se
     */
    public static void setCurrentStoreId(String storeId) {
        Log.debugf("Setting current store ID to: %s", storeId);
        CURRENT_STORE_ID.set(storeId);
    }
}
