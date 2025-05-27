package dev.tiodati.saas.gocommerce.platform.entity;

/**
 * Represents the various statuses a store can be in.
 */
public enum StoreStatus {
    /** Store is pending setup or approval. */
    PENDING,
    /** Store is active and operational. */
    ACTIVE,
    /** Store is temporarily inactive, can be reactivated. */
    INACTIVE,
    /** Store is suspended due to policy violations or other reasons. */
    SUSPENDED
}
