package dev.tiodati.saas.gocommerce.platform.entity;

public enum StoreStatus {
    CREATING,        // Initial state when store is being created
    PROVISIONING,    // Resources being allocated
    ACTIVE,          // Store is operational and accessible
    SUSPENDED,       // Store is temporarily disabled
    DELETED          // Store has been removed (soft delete)
}
