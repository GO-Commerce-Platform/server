package dev.tiodati.saas.gocommerce.cart.repository;

import dev.tiodati.saas.gocommerce.cart.entity.CartStatus;
import dev.tiodati.saas.gocommerce.cart.entity.ShoppingCart;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ShoppingCart entity operations.
 * Provides methods for cart management, retrieval, and cleanup.
 */
@ApplicationScoped
public class ShoppingCartRepository implements PanacheRepositoryBase<ShoppingCart, UUID> {

    /**
     * Find active shopping cart by customer ID.
     * Returns the current active cart for the customer.
     *
     * @param customerId the customer ID
     * @return optional shopping cart
     */
    public Optional<ShoppingCart> findActiveByCustomerId(UUID customerId) {
        return find("customerId = ?1 and status = ?2", customerId, CartStatus.ACTIVE)
                .firstResultOptional();
    }

    /**
     * Find shopping cart by session ID.
     * Used for guest checkout scenarios.
     *
     * @param sessionId the session ID
     * @return optional shopping cart
     */
    public Optional<ShoppingCart> findBySessionId(String sessionId) {
        return find("sessionId = ?1 and status = ?2", sessionId, CartStatus.ACTIVE)
                .firstResultOptional();
    }

    /**
     * Find all shopping carts for a customer.
     * Includes both active and inactive carts.
     *
     * @param customerId the customer ID
     * @param page       pagination information
     * @return list of shopping carts
     */
    public List<ShoppingCart> findByCustomerId(UUID customerId, Page page) {
        return find("customerId = ?1", customerId)
                .page(page)
                .list();
    }

    /**
     * Find abandoned shopping carts.
     * Carts that haven't been updated within the specified time period.
     *
     * @param abandonedSince the cutoff date for abandoned carts
     * @return list of abandoned shopping carts
     */
    public List<ShoppingCart> findAbandoned(LocalDateTime abandonedSince) {
        return find("status = ?1 and updatedAt < ?2", CartStatus.ACTIVE, abandonedSince)
                .list();
    }

    /**
     * Find shopping carts with specific total range.
     * Useful for analytics and targeted marketing.
     *
     * @param minTotal minimum total amount
     * @param maxTotal maximum total amount
     * @return list of shopping carts
     */
    public List<ShoppingCart> findByTotalRange(double minTotal, double maxTotal) {
        return find("totalAmount >= ?1 and totalAmount <= ?2", minTotal, maxTotal)
                .list();
    }

    /**
     * Count active shopping carts.
     *
     * @return number of active carts
     */
    public long countActiveCarts() {
        return count("status = ?1", CartStatus.ACTIVE);
    }

    /**
     * Deactivate shopping cart.
     * Marks the cart as inactive instead of deleting it.
     *
     * @param cartId the cart ID
     * @return number of affected rows
     */
    public int deactivateCart(UUID cartId) {
        return update("status = ?1, updatedAt = CURRENT_TIMESTAMP where id = ?2",
                CartStatus.ABANDONED, cartId);
    }

    /**
     * Clean up old inactive carts.
     * Permanently removes carts that have been inactive for a long period.
     *
     * @param cutoffDate date before which inactive carts should be deleted
     * @return number of deleted carts
     */
    public long cleanupOldCarts(LocalDateTime cutoffDate) {
        return delete("status != ?1 and updatedAt < ?2", CartStatus.ACTIVE, cutoffDate);
    }

    /**
     * Update cart total amount.
     * Recalculates and updates the total amount for the cart.
     *
     * @param cartId      the cart ID
     * @param totalAmount the new total amount
     * @return number of affected rows
     */
    public int updateTotalAmount(UUID cartId, double totalAmount) {
        return update("totalAmount = ?1, updatedAt = CURRENT_TIMESTAMP where id = ?2",
                totalAmount, cartId);
    }

    /**
     * Transfer guest cart to customer.
     * Associates a guest cart with a customer account.
     *
     * @param sessionId  the session ID of the guest cart
     * @param customerId the customer ID to associate with
     * @return number of affected rows
     */
    public int transferGuestCartToCustomer(String sessionId, UUID customerId) {
        return update("customerId = ?1, sessionId = null, updatedAt = CURRENT_TIMESTAMP "
                + "where sessionId = ?2 and status = ?3",
                customerId, sessionId, CartStatus.ACTIVE);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
