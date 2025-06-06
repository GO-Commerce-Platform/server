package dev.tiodati.saas.gocommerce.customer.entity;

/**
 * Enumeration representing the various states a customer account can be in.
 *
 * <p>
 * This enum defines the account status states for customer management:
 * </p>
 * <ul>
 * <li>ACTIVE - Customer account is active and can place orders</li>
 * <li>INACTIVE - Customer account is inactive but can be reactivated</li>
 * <li>SUSPENDED - Customer account is suspended due to policy violations</li>
 * </ul>
 *
 * @since 1.0
 */
public enum CustomerStatus {
    /**
     * Customer account is active and fully functional.
     */
    ACTIVE,

    /**
     * Customer account is inactive but can be reactivated.
     */
    INACTIVE,

    /**
     * Customer account is suspended due to policy violations or other issues.
     */
    SUSPENDED
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
