package dev.tiodati.saas.gocommerce.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import dev.tiodati.saas.gocommerce.order.dto.CreateOrderDto;
import dev.tiodati.saas.gocommerce.order.dto.CreateOrderFromCartDto;
import dev.tiodati.saas.gocommerce.order.dto.OrderDto;
import dev.tiodati.saas.gocommerce.order.dto.OrderItemDto;
import dev.tiodati.saas.gocommerce.order.entity.OrderHeader;
import dev.tiodati.saas.gocommerce.order.entity.OrderItem;
import dev.tiodati.saas.gocommerce.order.entity.OrderStatus;
import dev.tiodati.saas.gocommerce.order.repository.OrderItemRepository;
import dev.tiodati.saas.gocommerce.order.repository.OrderRepository;
import dev.tiodati.saas.gocommerce.product.entity.Product;
import dev.tiodati.saas.gocommerce.cart.entity.ShoppingCart;
import dev.tiodati.saas.gocommerce.cart.entity.CartItem;
import dev.tiodati.saas.gocommerce.cart.entity.CartStatus;
import dev.tiodati.saas.gocommerce.cart.repository.ShoppingCartRepository;
import dev.tiodati.saas.gocommerce.cart.repository.CartItemRepository;
import dev.tiodati.saas.gocommerce.inventory.service.InventoryService;
import java.util.Map;
import java.util.HashMap;
@ApplicationScoped
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /**
     * Repository for order data access operations.
     */
    private final OrderRepository orderRepository;

    /**
     * Repository for order item data access operations.
     */
    private final OrderItemRepository orderItemRepository;

    /**
     * Repository for shopping cart data access operations.
     */
    private final ShoppingCartRepository cartRepository;

    /**
     * Repository for cart item data access operations.
     */
    private final CartItemRepository cartItemRepository;

    /**
     * Service for inventory management operations.
     */
    private final InventoryService inventoryService;

    @Override
    public List<OrderDto> listOrders(UUID storeId, int page, int size, String statusId) {
        Log.infof("Listing orders for store %s (page=%d, size=%d, statusId=%s)",
                storeId, page, size, statusId);

        List<OrderHeader> orders;
        var pageable = Page.of(page, size);

        if (statusId != null) {
            var status = new OrderStatus();
            status.setId(statusId);
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            orders = orderRepository.findAll().page(pageable).list();
        }

        return orders.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public Optional<OrderDto> findOrder(UUID storeId, UUID orderId) {
        Log.infof("Finding order %s for store %s", orderId, storeId);

        return orderRepository.findByIdOptional(orderId)
                .map(this::mapToDto);
    }

    @Override
    public Optional<OrderDto> findOrderByNumber(UUID storeId, String orderNumber) {
        Log.infof("Finding order by number %s for store %s", orderNumber, storeId);

        return orderRepository.findByOrderNumber(orderNumber)
                .map(this::mapToDto);
    }

    @Override
    public List<OrderDto> getCustomerOrders(UUID storeId, UUID customerId, int page, int size) {
        Log.infof("Getting customer orders for customer %s in store %s (page=%d, size=%d)",
                customerId, storeId, page, size);

        var pageable = Page.of(page, size);
        var orders = orderRepository.findByCustomerId(customerId, pageable);

        return orders.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderDto createOrder(UUID storeId, CreateOrderDto orderDto) {
        Log.infof("Creating order for store %s, customer %s", storeId, orderDto.customerId());

        // Generate unique order number
        var orderNumber = generateOrderNumber();

        // Calculate totals
        var subtotal = calculateSubtotal(orderDto.items());
        var taxAmount = calculateTax(subtotal);
        var shippingAmount = calculateShipping(orderDto);
        var totalAmount = subtotal.add(taxAmount).add(shippingAmount);

        // Create order header
        var order = OrderHeader.builder()
                .orderNumber(orderNumber)
                .customerId(orderDto.customerId())
                .status(getDefaultOrderStatus())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .currencyCode("USD")
                .locale("en")
                .shippingFirstName(orderDto.shippingFirstName())
                .shippingLastName(orderDto.shippingLastName())
                .shippingAddressLine1(orderDto.shippingAddressLine1())
                .shippingAddressLine2(orderDto.shippingAddressLine2())
                .shippingCity(orderDto.shippingCity())
                .shippingStateProvince(orderDto.shippingStateProvince())
                .shippingPostalCode(orderDto.shippingPostalCode())
                .shippingCountry(orderDto.shippingCountry())
                .shippingPhone(orderDto.shippingPhone())
                .billingFirstName(orderDto.billingFirstName())
                .billingLastName(orderDto.billingLastName())
                .billingAddressLine1(orderDto.billingAddressLine1())
                .billingAddressLine2(orderDto.billingAddressLine2())
                .billingCity(orderDto.billingCity())
                .billingStateProvince(orderDto.billingStateProvince())
                .billingPostalCode(orderDto.billingPostalCode())
                .billingCountry(orderDto.billingCountry())
                .billingPhone(orderDto.billingPhone())
                .notes(orderDto.notes())
                .build();

        orderRepository.persist(order);

        // Create order items
        for (var itemDto : orderDto.items()) {
            var item = createOrderItem(order, itemDto);
            orderRepository.getEntityManager().persist(item);
        }

        return mapToDto(order);
    }

    @Override
    @Transactional
    public OrderDto createOrderFromCart(UUID storeId, CreateOrderFromCartDto createOrderDto) {
        Log.infof("Creating order from cart %s for store %s, customer %s", 
                createOrderDto.cartId(), storeId, createOrderDto.customerId());

        // 1. Validate and retrieve the shopping cart
        var cart = validateAndRetrieveCart(createOrderDto.cartId(), createOrderDto.customerId());
        
        // 2. Get cart items
        var cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Shopping cart is empty");
        }

        // 3. Check stock availability for all items
        validateStockAvailability(storeId, cartItems);

        // 4. Reserve stock for all items
        var reservationIds = reserveStockForCartItems(storeId, cartItems);

        try {
            // 5. Create the order
            var order = createOrderFromCartData(storeId, createOrderDto, cart, cartItems);

            // 6. Confirm stock reservations (convert to actual stock reduction)
            confirmStockReservations(storeId, reservationIds, "Order creation: " + order.orderNumber());

            // 7. Clear the cart if requested
            if (createOrderDto.shouldClearCartAfterOrder()) {
                clearCart(cart);
            }

            Log.infof("Successfully created order %s from cart %s", order.orderNumber(), cart.getId());
            return order;
        } catch (Exception e) {
            // If anything goes wrong, release the reserved stock
            releaseStockReservations(storeId, reservationIds);
            throw e;
        }
    }

    @Override
    @Transactional
    public Optional<OrderDto> updateOrderStatus(UUID storeId, UUID orderId, String newStatusId) {
        Log.infof("Updating order %s status to %s for store %s", orderId, newStatusId, storeId);

        return orderRepository.findByIdOptional(orderId)
                .map(order -> {
                    var newStatus = new OrderStatus();
                    newStatus.setId(newStatusId);
                    order.setStatus(newStatus);
                    orderRepository.persist(order);
                    return mapToDto(order);
                });
    }

    @Override
    @Transactional
    public Optional<OrderDto> markOrderShipped(UUID storeId, UUID orderId, Instant shippedDate) {
        Log.infof("Marking order %s as shipped for store %s", orderId, storeId);

        return orderRepository.findByIdOptional(orderId)
                .map(order -> {
                    order.setShippedDate(shippedDate);

                    // Update status to SHIPPED if current status allows it
                    var shippedStatus = new OrderStatus();
                    shippedStatus.setId("SHIPPED");
                    order.setStatus(shippedStatus);

                    orderRepository.persist(order);
                    return mapToDto(order);
                });
    }

    @Override
    @Transactional
    public Optional<OrderDto> markOrderDelivered(UUID storeId, UUID orderId, Instant deliveredDate) {
        Log.infof("Marking order %s as delivered for store %s", orderId, storeId);

        return orderRepository.findByIdOptional(orderId)
                .map(order -> {
                    order.setDeliveredDate(deliveredDate);

                    // Update status to DELIVERED
                    var deliveredStatus = new OrderStatus();
                    deliveredStatus.setId("DELIVERED");
                    order.setStatus(deliveredStatus);

                    orderRepository.persist(order);
                    return mapToDto(order);
                });
    }

    @Override
    public List<OrderDto> getOrdersByDateRange(UUID storeId, Instant startDate, Instant endDate, int page, int size) {
        Log.infof("Getting orders by date range for store %s (from %s to %s, page=%d, size=%d)",
                storeId, startDate, endDate, page, size);

        var pageable = Page.of(page, size);
        var orders = orderRepository.findByDateRange(startDate, endDate, pageable);

        return orders.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public long countOrdersByStatus(UUID storeId, String statusId) {
        Log.infof("Counting orders by status %s for store %s", statusId, storeId);

        var status = new OrderStatus();
        status.setId(statusId);
        return orderRepository.countByStatus(status);
    }

    @Override
    @Transactional
    public Optional<OrderDto> cancelOrder(UUID storeId, UUID orderId, String reason) {
        Log.infof("Cancelling order %s for store %s (reason: %s)", orderId, storeId, reason);

        return orderRepository.findByIdOptional(orderId)
                .map(order -> {
                    // Check if order can be cancelled (e.g., not shipped yet)
                    if (order.getShippedDate() != null) {
                        throw new IllegalStateException("Cannot cancel order that has already been shipped");
                    }

                    // Update status to CANCELLED
                    var cancelledStatus = new OrderStatus();
                    cancelledStatus.setId("CANCELLED");
                    order.setStatus(cancelledStatus);

                    // Add cancellation reason to notes
                    var existingNotes = order.getNotes() != null ? order.getNotes() : "";
                    order.setNotes(existingNotes + "\nCancelled: " + reason);

                    orderRepository.persist(order);
                    return mapToDto(order);
                });
    }

    /**
     * Maps an OrderHeader entity to OrderDto.
     *
     * @param order the OrderHeader entity to map
     * @return the corresponding OrderDto
     */
    private OrderDto mapToDto(OrderHeader order) {
        // Load order items for this order
        List<OrderItemDto> items = orderItemRepository.findByOrderId(order.getId())
                .stream()
                .map(this::mapOrderItemToDto)
                .toList();

        return new OrderDto(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getStatus() != null ? order.getStatus().getId() : null,
                order.getStatus() != null ? order.getStatus().getName() : null,
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getShippingAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getCurrencyCode(),
                order.getLocale(),
                order.getShippingFirstName(),
                order.getShippingLastName(),
                order.getShippingAddressLine1(),
                order.getShippingAddressLine2(),
                order.getShippingCity(),
                order.getShippingStateProvince(),
                order.getShippingPostalCode(),
                order.getShippingCountry(),
                order.getShippingPhone(),
                order.getBillingFirstName(),
                order.getBillingLastName(),
                order.getBillingAddressLine1(),
                order.getBillingAddressLine2(),
                order.getBillingCity(),
                order.getBillingStateProvince(),
                order.getBillingPostalCode(),
                order.getBillingCountry(),
                order.getBillingPhone(),
                order.getOrderDate(),
                order.getShippedDate(),
                order.getDeliveredDate(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getVersion(),
                items);
    }

    /**
     * Maps an OrderItem entity to OrderItemDto.
     *
     * @param orderItem the OrderItem entity to map
     * @return the corresponding OrderItemDto
     */
    private OrderItemDto mapOrderItemToDto(OrderItem orderItem) {
        return new OrderItemDto(
                orderItem.getId(),
                orderItem.getOrder().getId(),
                orderItem.getProduct().getId(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getTotalPrice(),
                orderItem.getProductName(),
                orderItem.getProductSku(),
                orderItem.getCreatedAt(),
                orderItem.getUpdatedAt(),
                orderItem.getVersion()
        );
    }

    /**
     * Creates an OrderItem entity from the create DTO.
     *
     * @param order   The parent order
     * @param itemDto The item creation DTO
     * @return The created OrderItem entity
     */
    private OrderItem createOrderItem(OrderHeader order, CreateOrderDto.CreateOrderItemDto itemDto) {
        // Fetch the actual product to get its details
        var product = orderRepository.getEntityManager().find(Product.class, itemDto.productId());
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + itemDto.productId());
        }

        var totalPrice = itemDto.unitPrice().multiply(BigDecimal.valueOf(itemDto.quantity()));

        return OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(itemDto.quantity())
                .unitPrice(itemDto.unitPrice())
                .totalPrice(totalPrice)
                .productName(product.getName())
                .productSku(product.getSku())
                .build();
    }

    /**
     * Generates a unique order number.
     *
     * @return A unique order number
     */
    private String generateOrderNumber() {
        // Simple implementation - in production would be more sophisticated
        return "ORD-" + System.currentTimeMillis();
    }

    /**
     * Calculates subtotal from order items.
     *
     * @param items Order items
     * @return Subtotal amount
     */
    private BigDecimal calculateSubtotal(List<CreateOrderDto.CreateOrderItemDto> items) {
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates tax amount.
     *
     * @param subtotal Subtotal amount
     * @return Tax amount
     */
    private BigDecimal calculateTax(BigDecimal subtotal) {
        // Simple 10% tax rate - in production would be more sophisticated
        return subtotal.multiply(BigDecimal.valueOf(0.10));
    }

    /**
     * Calculates shipping amount.
     *
     * @param orderDto Order data
     * @return Shipping amount
     */
    private BigDecimal calculateShipping(CreateOrderDto orderDto) {
        // Simple flat rate shipping - in production would be more sophisticated
        return BigDecimal.valueOf(9.99);
    }

    /**
     * Gets the default order status for new orders.
     *
     * @return Default OrderStatus
     */
    private OrderStatus getDefaultOrderStatus() {
        var status = new OrderStatus();
        status.setId("PENDING");
        status.setName("Pending");
        return status;
    }

    // Helper methods for cart-to-order conversion

    /**
     * Validates and retrieves a shopping cart.
     *
     * @param cartId     The cart ID
     * @param customerId The customer ID
     * @return The validated shopping cart
     * @throws IllegalArgumentException if cart is not found, not active, expired, or doesn't belong to customer
     */
    private ShoppingCart validateAndRetrieveCart(UUID cartId, UUID customerId) {
        var cart = cartRepository.findByIdOptional(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Shopping cart not found"));

        // Check if cart belongs to the customer
        if (!cart.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Shopping cart does not belong to the specified customer");
        }

        // Check if cart is active
        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new IllegalArgumentException("Shopping cart is not active");
        }

        // Check if cart is not expired
        if (cart.getExpiresAt() != null && cart.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Shopping cart has expired");
        }

        return cart;
    }

    /**
     * Validates stock availability for all cart items.
     *
     * @param storeId   The store ID
     * @param cartItems The cart items to validate
     * @throws IllegalStateException if insufficient stock for any item
     */
    private void validateStockAvailability(UUID storeId, List<CartItem> cartItems) {
        for (var cartItem : cartItems) {
            var productId = cartItem.getProduct().getId();
            var requestedQuantity = cartItem.getQuantity();
            
            var hasSufficientStock = inventoryService.hasSufficientStock(storeId, productId, requestedQuantity);
            if (!hasSufficientStock) {
                throw new IllegalStateException(
                    String.format("Insufficient stock for product %s (SKU: %s). Requested: %d", 
                            productId, cartItem.getProduct().getSku(), requestedQuantity));
            }
        }
    }

    /**
     * Reserves stock for all cart items.
     *
     * @param storeId   The store ID
     * @param cartItems The cart items
     * @return Map of product IDs to reservation IDs
     */
    private Map<UUID, String> reserveStockForCartItems(UUID storeId, List<CartItem> cartItems) {
        Map<UUID, String> reservationIds = new HashMap<>();
        
        for (var cartItem : cartItems) {
            var productId = cartItem.getProduct().getId();
            var reservationId = generateReservationId(productId);
            
            var success = inventoryService.reserveStock(storeId, productId, cartItem.getQuantity(), reservationId);
            if (!success) {
                // If any reservation fails, release all previous reservations
                releaseStockReservations(storeId, reservationIds);
                throw new IllegalStateException(
                    String.format("Failed to reserve stock for product %s (SKU: %s)", 
                            productId, cartItem.getProduct().getSku()));
            }
            
            reservationIds.put(productId, reservationId);
        }
        
        return reservationIds;
    }

    /**
     * Releases stock reservations (used in error scenarios).
     *
     * @param storeId       The store ID
     * @param reservationIds Map of product IDs to reservation IDs
     */
    private void releaseStockReservations(UUID storeId, Map<UUID, String> reservationIds) {
        for (var entry : reservationIds.entrySet()) {
            try {
                inventoryService.releaseStockReservation(storeId, entry.getValue());
            } catch (Exception e) {
                // Log but don't throw - we're already in error handling
                Log.warnf("Failed to release stock reservation %s for product %s: %s", 
                        entry.getValue(), entry.getKey(), e.getMessage());
            }
        }
    }

    /**
     * Confirms stock reservations (converts reservations to actual stock reduction).
     *
     * @param storeId       The store ID
     * @param reservationIds Map of product IDs to reservation IDs
     * @param reason        Reason for the stock confirmation
     */
    private void confirmStockReservations(UUID storeId, Map<UUID, String> reservationIds, String reason) {
        for (var entry : reservationIds.entrySet()) {
            var success = inventoryService.confirmStockReservation(storeId, entry.getValue(), reason);
            if (!success) {
                Log.warnf("Failed to confirm stock reservation %s for product %s", 
                        entry.getValue(), entry.getKey());
            }
        }
    }

    /**
     * Generates a unique reservation ID for stock reservation.
     *
     * @param productId The product ID
     * @return A unique reservation ID
     */
    private String generateReservationId(UUID productId) {
        return "ORDER-" + System.currentTimeMillis() + "-" + productId.toString().substring(0, 8);
    }

    /**
     * Creates an order from cart data.
     *
     * @param storeId        The store ID
     * @param createOrderDto The order creation data
     * @param cart           The shopping cart
     * @param cartItems      The cart items
     * @return The created order DTO
     */
    private OrderDto createOrderFromCartData(UUID storeId, CreateOrderFromCartDto createOrderDto, 
                                              ShoppingCart cart, List<CartItem> cartItems) {
        // Generate unique order number
        var orderNumber = generateOrderNumber();

        // Calculate totals from cart items
        var subtotal = calculateSubtotalFromCartItems(cartItems);
        var taxAmount = calculateTax(subtotal);
        var shippingAmount = calculateShippingFromCart(createOrderDto);
        var totalAmount = subtotal.add(taxAmount).add(shippingAmount);

        // Create order header
        var order = OrderHeader.builder()
                .orderNumber(orderNumber)
                .customerId(createOrderDto.customerId())
                .status(getDefaultOrderStatus())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .currencyCode("USD")
                .locale("en")
                .shippingFirstName(createOrderDto.shippingFirstName())
                .shippingLastName(createOrderDto.shippingLastName())
                .shippingAddressLine1(createOrderDto.shippingAddressLine1())
                .shippingAddressLine2(createOrderDto.shippingAddressLine2())
                .shippingCity(createOrderDto.shippingCity())
                .shippingStateProvince(createOrderDto.shippingStateProvince())
                .shippingPostalCode(createOrderDto.shippingPostalCode())
                .shippingCountry(createOrderDto.shippingCountry())
                .shippingPhone(createOrderDto.shippingPhone())
                .billingFirstName(createOrderDto.billingFirstName())
                .billingLastName(createOrderDto.billingLastName())
                .billingAddressLine1(createOrderDto.billingAddressLine1())
                .billingAddressLine2(createOrderDto.billingAddressLine2())
                .billingCity(createOrderDto.billingCity())
                .billingStateProvince(createOrderDto.billingStateProvince())
                .billingPostalCode(createOrderDto.billingPostalCode())
                .billingCountry(createOrderDto.billingCountry())
                .billingPhone(createOrderDto.billingPhone())
                .notes(createOrderDto.notes())
                .build();

        orderRepository.persist(order);

        // Create order items from cart items
        for (var cartItem : cartItems) {
            var orderItem = createOrderItemFromCartItem(order, cartItem);
            orderRepository.getEntityManager().persist(orderItem);
        }

        return mapToDto(order);
    }

    /**
     * Creates an OrderItem from a CartItem.
     *
     * @param order    The parent order
     * @param cartItem The cart item
     * @return The created OrderItem entity
     */
    private OrderItem createOrderItemFromCartItem(OrderHeader order, CartItem cartItem) {
        var totalPrice = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return OrderItem.builder()
                .order(order)
                .product(cartItem.getProduct())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .totalPrice(totalPrice)
                .productName(cartItem.getProduct().getName())
                .productSku(cartItem.getProduct().getSku())
                .build();
    }

    /**
     * Calculates subtotal from cart items.
     *
     * @param cartItems The cart items
     * @return Subtotal amount
     */
    private BigDecimal calculateSubtotalFromCartItems(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates shipping amount for cart order.
     *
     * @param createOrderDto Order creation data
     * @return Shipping amount
     */
    private BigDecimal calculateShippingFromCart(CreateOrderFromCartDto createOrderDto) {
        // Simple flat rate shipping - in production would be more sophisticated
        return BigDecimal.valueOf(9.99);
    }

    /**
     * Clears the shopping cart by removing all items and updating status.
     *
     * @param cart The cart to clear
     */
    private void clearCart(ShoppingCart cart) {
        // Remove all cart items
        var cartItems = cartItemRepository.findByCartId(cart.getId());
        for (var cartItem : cartItems) {
            cartItemRepository.delete(cartItem);
        }

        // Update cart status to CONVERTED
        cart.setStatus(CartStatus.CONVERTED);
        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.persist(cart);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
