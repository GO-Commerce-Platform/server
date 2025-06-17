package dev.tiodati.saas.gocommerce.order.repository;

import dev.tiodati.saas.gocommerce.order.entity.OrderHeader;
import dev.tiodati.saas.gocommerce.order.entity.OrderStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for OrderHeader entity operations.
 * Provides database access methods for order management.
 */
@ApplicationScoped
public class OrderRepository implements PanacheRepositoryBase<OrderHeader, UUID> {

    /**
     * Find order by order number.
     *
     * @param orderNumber the order number
     * @return optional order
     */
    public Optional<OrderHeader> findByOrderNumber(String orderNumber) {
        return find("orderNumber = ?1", orderNumber)
                .firstResultOptional();
    }

    /**
     * Find orders by customer ID with pagination.
     *
     * @param customerId the customer ID
     * @param page       the page information
     * @return list of customer orders
     */
    public List<OrderHeader> findByCustomerId(UUID customerId, Page page) {
        return find("customerId = ?1 ORDER BY orderDate DESC", customerId)
                .page(page)
                .list();
    }

    /**
     * Find orders by status with pagination.
     *
     * @param status the order status
     * @param page   the page information
     * @return list of orders with the specified status
     */
    public List<OrderHeader> findByStatus(OrderStatus status, Page page) {
        return find("status = ?1 ORDER BY orderDate DESC", status)
                .page(page)
                .list();
    }

    /**
     * Find orders within a date range.
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @param page      the page information
     * @return list of orders within the date range
     */
    public List<OrderHeader> findByDateRange(Instant startDate, Instant endDate, Page page) {
        return find("orderDate >= ?1 AND orderDate <= ?2 ORDER BY orderDate DESC", startDate, endDate)
                .page(page)
                .list();
    }

    /**
     * Find recent orders for a customer.
     *
     * @param customerId the customer ID
     * @param limit      the maximum number of orders to return
     * @return list of recent orders
     */
    public List<OrderHeader> findRecentByCustomerId(UUID customerId, int limit) {
        return find("customerId = ?1 ORDER BY orderDate DESC", customerId)
                .range(0, limit - 1)
                .list();
    }

    /**
     * Count orders by status.
     *
     * @param status the order status
     * @return count of orders with the specified status
     */
    public long countByStatus(OrderStatus status) {
        return count("status = ?1", status);
    }

    /**
     * Count orders for a customer.
     *
     * @param customerId the customer ID
     * @return count of orders for the customer
     */
    public long countByCustomerId(UUID customerId) {
        return count("customerId = ?1", customerId);
    }

    /**
     * Update order status.
     *
     * @param orderId the order ID
     * @param status  the new status
     * @return number of updated records
     */
    public int updateStatus(UUID orderId, OrderStatus status) {
        return update("status = ?1, updatedAt = ?2 WHERE id = ?3",
                status, Instant.now(), orderId);
    }

    /**
     * Find orders that need to be shipped (confirmed but not shipped).
     *
     * @param page the page information
     * @return list of orders ready for shipping
     */
    public List<OrderHeader> findOrdersToShip(Page page) {
        return find("status.id = 'CONFIRMED' AND shippedDate IS NULL ORDER BY orderDate", page)
                .list();
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
