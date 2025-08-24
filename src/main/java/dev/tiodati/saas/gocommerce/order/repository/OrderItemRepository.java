package dev.tiodati.saas.gocommerce.order.repository;

import dev.tiodati.saas.gocommerce.order.entity.OrderHeader;
import dev.tiodati.saas.gocommerce.order.entity.OrderItem;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for OrderItem entity operations.
 * Provides database access methods for order item management.
 */
@ApplicationScoped
public class OrderItemRepository implements PanacheRepositoryBase<OrderItem, UUID> {

    /**
     * Find all order items for a specific order.
     *
     * @param orderId the order ID
     * @return list of order items
     */
    public List<OrderItem> findByOrderId(UUID orderId) {
        return find("order.id = ?1 ORDER BY createdAt", orderId).list();
    }

    /**
     * Find all order items for a specific order.
     *
     * @param order the order header
     * @return list of order items
     */
    public List<OrderItem> findByOrder(OrderHeader order) {
        return find("order = ?1 ORDER BY createdAt", order).list();
    }

    /**
     * Find order items by product ID.
     *
     * @param productId the product ID
     * @return list of order items containing the product
     */
    public List<OrderItem> findByProductId(UUID productId) {
        return find("product.id = ?1 ORDER BY createdAt DESC", productId).list();
    }

    /**
     * Count order items for a specific order.
     *
     * @param orderId the order ID
     * @return count of order items
     */
    public long countByOrderId(UUID orderId) {
        return count("order.id = ?1", orderId);
    }

    /**
     * Delete all order items for a specific order.
     *
     * @param orderId the order ID
     * @return number of deleted items
     */
    public long deleteByOrderId(UUID orderId) {
        return delete("order.id = ?1", orderId);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
