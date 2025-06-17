package dev.tiodati.saas.gocommerce.cart.entity;

/**
 * Enumeration representing the various states a shopping cart can be in.
 *
 * <p>
 * This enum defines the lifecycle states of a shopping cart within the
 * e-commerce
 * system:
 * </p>
 * <ul>
 * <li>ACTIVE - Cart is active and available for modifications</li>
 * <li>ABANDONED - Cart has been abandoned by the customer (inactive for a
 * period)</li>
 * <li>CONVERTED - Cart has been converted to an order</li>
 * <li>EXPIRED - Cart has expired and is no longer valid</li>
 * </ul>
 *
 * @since 1.0
 */
public enum CartStatus {
    /**
     * Cart is active and available for modifications.
     */
    ACTIVE,

    /**
     * Cart has been abandoned by the customer (inactive for a period).
     */
    ABANDONED,

    /**
     * Cart has been converted to an order.
     */
    CONVERTED,

    /**
     * Cart has expired and is no longer valid.
     */
    EXPIRED
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
