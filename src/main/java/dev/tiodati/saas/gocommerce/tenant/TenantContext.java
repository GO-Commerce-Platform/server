package dev.tiodati.saas.gocommerce.tenant;

import jakarta.enterprise.context.RequestScoped;

/**
 * Manages the tenant context for the current request or operation.
 * This class allows explicit tenant context switching when needed.
 */
@RequestScoped
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    
    /**
     * Sets the current tenant ID for the executing thread.
     * 
     * @param tenantId The tenant identifier (schema name)
     */
    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }
    
    /**
     * Gets the current tenant ID for the executing thread.
     * 
     * @return The current tenant identifier or null if not set
     */
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }
    
    /**
     * Clears the current tenant ID from the executing thread.
     * This should be called at the end of operations to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}