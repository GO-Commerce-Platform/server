package dev.tiodati.saas.gocommerce.product.entity;

/**
 * Enumeration representing the various states a product can be in.
 *
 * <p>
 * This enum defines the lifecycle states of a product within the e-commerce
 * system:
 * </p>
 * <ul>
 * <li>DRAFT - Product is being created/edited but not yet available for
 * purchase</li>
 * <li>ACTIVE - Product is live and available for purchase</li>
 * <li>ARCHIVED - Product is no longer available but preserved for historical
 * records</li>
 * </ul>
 *
 * @since 1.0
 */
public enum ProductStatus {
    /**
     * Product is in draft state - being created or edited but not yet published.
     */
    DRAFT,

    /**
     * Product is active and available for purchase.
     */
    ACTIVE,

    /**
     * Product is archived - no longer available but preserved for historical
     * records.
     */
    ARCHIVED
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
