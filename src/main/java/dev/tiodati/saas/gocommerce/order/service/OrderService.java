package dev.tiodati.saas.gocommerce.order.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.order.dto.CreateOrderDto;
import dev.tiodati.saas.gocommerce.order.dto.CreateOrderFromCartDto;
import dev.tiodati.saas.gocommerce.order.dto.OrderDto;

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
     * This method converts cart items to order items, validates stock availability,
     * updates inventory, and optionally clears the cart after successful order creation.
     *
     * @param storeId        The store ID
     * @param cartOrderDto   Cart order conversion data
     * @return The created order
     * @throws IllegalArgumentException if cart is not found or empty
     * @throws IllegalStateException    if insufficient stock or cart validation fails
     */
    OrderDto createOrderFromCart(UUID storeId, CreateOrderFromCartDto cartOrderDto);

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
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
