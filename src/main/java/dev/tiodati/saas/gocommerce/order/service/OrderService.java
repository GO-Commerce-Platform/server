package dev.tiodati.saas.gocommerce.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.order.dto.CreateOrderDto;
import dev.tiodati.saas.gocommerce.order.dto.CreateOrderFromCartDto;
import dev.tiodati.saas.gocommerce.order.dto.CreateRefundDto;
import dev.tiodati.saas.gocommerce.order.dto.OrderDto;
import dev.tiodati.saas.gocommerce.order.dto.RefundDto;

/**
 * Service interface for order management operations.
 * Provides methods for creating, retrieving, updating, and managing orders.
 */
public interface OrderService {

    /**
     * List orders for a store with pagination and optional status filter.
     *
     * @param storeId  The store ID
     * @param page     Page number (0-based)
     * @param size     Page size
     * @param statusId Optional status filter
     * @return List of order DTOs
     */
    List<OrderDto> listOrders(UUID storeId, int page, int size, String statusId);

    /**
     * Find an order by ID.
     *
     * @param storeId The store ID
     * @param orderId The order ID
     * @return Optional containing the order if found
     */
    Optional<OrderDto> findOrder(UUID storeId, UUID orderId);

    /**
     * Find an order by order number.
     *
     * @param storeId     The store ID
     * @param orderNumber The order number
     * @return Optional containing the order if found
     */
    Optional<OrderDto> findOrderByNumber(UUID storeId, String orderNumber);

    /**
     * Get orders for a specific customer.
     *
     * @param storeId    The store ID
     * @param customerId The customer ID
     * @param page       Page number (0-based)
     * @param size       Page size
     * @return List of customer orders
     */
    List<OrderDto> getCustomerOrders(UUID storeId, UUID customerId, int page, int size);

    /**
     * Create a new order.
     *
     * @param storeId  The store ID
     * @param orderDto Order data
     * @return The created order
     */
    OrderDto createOrder(UUID storeId, CreateOrderDto orderDto);

    /**
     * Create a new order from a shopping cart.
     * Validates cart ownership, stock availability, and converts cart items to order items.
     * Optionally clears the cart after successful order creation.
     *
     * @param storeId           The store ID
     * @param createOrderDto    Order creation data with cart ID and shipping/billing info
     * @return The created order
     * @throws IllegalArgumentException if cart is not found, empty, expired, or customer doesn't own it
     * @throws IllegalStateException    if insufficient stock is available for any cart items
     */
    OrderDto createOrderFromCart(UUID storeId, CreateOrderFromCartDto createOrderDto);

    /**
     * Update order status.
     *
     * @param storeId     The store ID
     * @param orderId     The order ID
     * @param newStatusId The new status ID
     * @return Optional containing the updated order if found
     */
    Optional<OrderDto> updateOrderStatus(UUID storeId, UUID orderId, String newStatusId);

    /**
     * Mark order as shipped.
     *
     * @param storeId     The store ID
     * @param orderId     The order ID
     * @param shippedDate When the order was shipped
     * @return Optional containing the updated order if found
     */
    Optional<OrderDto> markOrderShipped(UUID storeId, UUID orderId, Instant shippedDate);

    /**
     * Mark order as delivered.
     *
     * @param storeId       The store ID
     * @param orderId       The order ID
     * @param deliveredDate When the order was delivered
     * @return Optional containing the updated order if found
     */
    Optional<OrderDto> markOrderDelivered(UUID storeId, UUID orderId, Instant deliveredDate);

    /**
     * Get orders within a date range.
     *
     * @param storeId   The store ID
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @param page      Page number (0-based)
     * @param size      Page size
     * @return List of orders within the date range
     */
    List<OrderDto> getOrdersByDateRange(UUID storeId, Instant startDate, Instant endDate, int page, int size);

    /**
     * Get count of orders by status.
     *
     * @param storeId  The store ID
     * @param statusId The order status ID
     * @return Count of orders with the specified status
     */
    long countOrdersByStatus(UUID storeId, String statusId);

    /**
     * Cancel an order.
     *
     * @param storeId The store ID
     * @param orderId The order ID
     * @param reason  Cancellation reason
     * @return Optional containing the updated order if found and cancellable
     */
    Optional<OrderDto> cancelOrder(UUID storeId, UUID orderId, String reason);

    /**
     * Create a refund for an order.
     *
     * @param storeId       The store ID
     * @param createRefundDto Refund creation data
     * @return The created refund
     * @throws IllegalArgumentException if order is not found or not refundable
     * @throws IllegalStateException if refund amount exceeds refundable amount
     */
    RefundDto createRefund(UUID storeId, CreateRefundDto createRefundDto);

    /**
     * Get refunds for an order.
     *
     * @param storeId The store ID
     * @param orderId The order ID
     * @return List of refunds for the order
     */
    List<RefundDto> getOrderRefunds(UUID storeId, UUID orderId);

    /**
     * Find a refund by ID.
     *
     * @param storeId  The store ID
     * @param refundId The refund ID
     * @return Optional containing the refund if found
     */
    Optional<RefundDto> findRefund(UUID storeId, UUID refundId);

    /**
     * Process a pending refund.
     *
     * @param storeId  The store ID
     * @param refundId The refund ID
     * @param processedAmount The amount actually processed (may differ from requested)
     * @param notes    Processing notes
     * @return Optional containing the processed refund if found
     */
    Optional<RefundDto> processRefund(UUID storeId, UUID refundId, BigDecimal processedAmount, String notes);
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
