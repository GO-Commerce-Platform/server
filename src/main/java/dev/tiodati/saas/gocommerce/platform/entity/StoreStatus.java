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
    SUSPENDED,
    /** Store is permanently closed and cannot be reactivated. */
    CLOSED,
    /** Store is archived, typically for historical purposes. */
    ARCHIVED,
    /** Store is in the process of being deleted. */
    DELETING,
    /** Store has been deleted and is no longer accessible. */
    DELETED
}
