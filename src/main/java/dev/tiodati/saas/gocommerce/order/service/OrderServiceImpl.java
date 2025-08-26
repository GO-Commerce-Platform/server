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
import dev.tiodati.saas.gocommerce.order.dto.CreateRefundDto;
import dev.tiodati.saas.gocommerce.order.dto.OrderDto;
import dev.tiodati.saas.gocommerce.order.dto.OrderItemDto;
import dev.tiodati.saas.gocommerce.order.dto.RefundDto;
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
import dev.tiodati.saas.gocommerce.inventory.dto.InventoryAdjustmentDto;
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
        var discountAmount = calculateDiscount(orderDto);
        var discountedSubtotal = subtotal.subtract(discountAmount);
        var taxAmount = calculateTax(discountedSubtotal);
        var shippingAmount = calculateShipping(orderDto);
        var totalAmount = discountedSubtotal.add(taxAmount).add(shippingAmount);

        // Create order header
        var order = OrderHeader.builder()
                .orderNumber(orderNumber)
                .customerId(orderDto.customerId())
                .status(getDefaultOrderStatus())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .discountAmount(discountAmount)
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

        // Create order items and reduce inventory
        for (var itemDto : orderDto.items()) {
            var item = createOrderItem(order, itemDto);
            orderRepository.getEntityManager().persist(item);
            
            // Reduce inventory stock for this item
            reduceInventoryForOrderItem(storeId, itemDto, order.getOrderNumber());
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
                    // Validate status transition
                    var currentStatusId = order.getStatus() != null ? order.getStatus().getId() : "PENDING";
                    validateStatusTransition(currentStatusId, newStatusId);
                    
                    var newStatus = new OrderStatus();
                    newStatus.setId(newStatusId);
                    newStatus.setName(getStatusName(newStatusId));
                    order.setStatus(newStatus);
                    
                    // Update relevant dates based on status
                    updateOrderDatesForStatus(order, newStatusId);
                    
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
                    // Validate status transition to SHIPPED
                    var currentStatusId = order.getStatus() != null ? order.getStatus().getId() : "PENDING";
                    validateStatusTransition(currentStatusId, "SHIPPED");
                    
                    order.setShippedDate(shippedDate);

                    // Update status to SHIPPED
                    var shippedStatus = new OrderStatus();
                    shippedStatus.setId("SHIPPED");
                    shippedStatus.setName("Shipped");
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
                    // Validate status transition to DELIVERED
                    var currentStatusId = order.getStatus() != null ? order.getStatus().getId() : "PENDING";
                    validateStatusTransition(currentStatusId, "DELIVERED");
                    
                    order.setDeliveredDate(deliveredDate);

                    // Update status to DELIVERED
                    var deliveredStatus = new OrderStatus();
                    deliveredStatus.setId("DELIVERED");
                    deliveredStatus.setName("Delivered");
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
                    // Validate that order can be cancelled
                    validateOrderCancellation(order);

                    // Restore inventory for all order items
                    restoreInventoryForCancelledOrder(storeId, order);

                    // Update status to CANCELLED
                    var cancelledStatus = new OrderStatus();
                    cancelledStatus.setId("CANCELLED");
                    cancelledStatus.setName("Cancelled");
                    order.setStatus(cancelledStatus);

                    // Add cancellation reason to notes
                    var existingNotes = order.getNotes() != null ? order.getNotes() : "";
                    order.setNotes(existingNotes + "\nCancelled: " + reason);

                    orderRepository.persist(order);
                    Log.infof("Successfully cancelled order %s and restored inventory", order.getOrderNumber());
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
     * Generates a unique order number using a combination of timestamp and UUID.
     * Format: ORD-YYYYMMDD-XXXXXXXX (where X is first 8 chars of UUID)
     *
     * @return A unique order number
     */
    private String generateOrderNumber() {
        var now = java.time.LocalDateTime.now();
        var datePrefix = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        var uniqueSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("ORD-%s-%s", datePrefix, uniqueSuffix);
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
     * Calculates discount amount for direct order creation.
     *
     * @param orderDto Order data
     * @return Discount amount
     */
    private BigDecimal calculateDiscount(CreateOrderDto orderDto) {
        // Basic discount calculation - in production would be more sophisticated
        // This could include promotional codes, customer discounts, bulk discounts, etc.
        
        // For now, check for any promotional code patterns in notes
        var notes = orderDto.notes();
        if (notes != null) {
            // Simple percentage discount based on promo codes in notes
            if (notes.contains("VIP10")) {
                var subtotal = calculateSubtotal(orderDto.items());
                return subtotal.multiply(BigDecimal.valueOf(0.10)); // 10% discount
            }
            if (notes.contains("WELCOME5")) {
                var subtotal = calculateSubtotal(orderDto.items());
                return subtotal.multiply(BigDecimal.valueOf(0.05)); // 5% discount
            }
            if (notes.contains("SAVE20")) {
                return BigDecimal.valueOf(20.00); // $20 flat discount
            }
        }
        
        // No discount applied
        return BigDecimal.ZERO;
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
        var discountAmount = calculateDiscountFromCart(createOrderDto, subtotal);
        var discountedSubtotal = subtotal.subtract(discountAmount);
        var taxAmount = calculateTax(discountedSubtotal);
        var shippingAmount = calculateShippingFromCart(createOrderDto);
        var totalAmount = discountedSubtotal.add(taxAmount).add(shippingAmount);

        // Create order header
        var order = OrderHeader.builder()
                .orderNumber(orderNumber)
                .customerId(createOrderDto.customerId())
                .status(getDefaultOrderStatus())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .discountAmount(discountAmount)
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
     * Calculates discount amount for cart-based order creation.
     *
     * @param createOrderDto Order creation data
     * @param subtotal       Order subtotal before discounts
     * @return Discount amount
     */
    private BigDecimal calculateDiscountFromCart(CreateOrderFromCartDto createOrderDto, BigDecimal subtotal) {
        // Basic discount calculation for cart orders - in production would be more sophisticated
        // This could integrate with a promotion engine, loyalty programs, customer tiers, etc.
        
        // Check for promotional codes in notes
        var notes = createOrderDto.notes();
        if (notes != null) {
            // Simple percentage discount based on promo codes in notes
            if (notes.contains("VIP10")) {
                return subtotal.multiply(BigDecimal.valueOf(0.10)); // 10% discount
            }
            if (notes.contains("WELCOME5")) {
                return subtotal.multiply(BigDecimal.valueOf(0.05)); // 5% discount
            }
            if (notes.contains("SAVE20")) {
                return BigDecimal.valueOf(20.00); // $20 flat discount
            }
            if (notes.contains("BULK15") && subtotal.compareTo(BigDecimal.valueOf(100.00)) >= 0) {
                return subtotal.multiply(BigDecimal.valueOf(0.15)); // 15% discount for orders over $100
            }
        }
        
        // Volume-based discounts
        if (subtotal.compareTo(BigDecimal.valueOf(500.00)) >= 0) {
            return subtotal.multiply(BigDecimal.valueOf(0.10)); // 10% discount for orders over $500
        } else if (subtotal.compareTo(BigDecimal.valueOf(200.00)) >= 0) {
            return subtotal.multiply(BigDecimal.valueOf(0.05)); // 5% discount for orders over $200
        }
        
        // No discount applied
        return BigDecimal.ZERO;
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

    // Business logic helper methods for order status management

    /**
     * Validates that a status transition is allowed based on business rules.
     *
     * @param currentStatusId The current order status
     * @param newStatusId     The target status
     * @throws IllegalArgumentException if the status transition is not allowed
     */
    private void validateStatusTransition(String currentStatusId, String newStatusId) {
        // Define allowed status transitions based on business rules
        Map<String, List<String>> allowedTransitions = Map.of(
                "PENDING", List.of("CONFIRMED", "CANCELLED"),
                "CONFIRMED", List.of("PROCESSING", "CANCELLED"),
                "PROCESSING", List.of("SHIPPED", "CANCELLED"),
                "SHIPPED", List.of("DELIVERED"),
                "DELIVERED", List.of(), // No transitions from delivered
                "CANCELLED", List.of()  // No transitions from cancelled
        );

        var allowedFromCurrent = allowedTransitions.getOrDefault(currentStatusId, List.of());
        if (!allowedFromCurrent.contains(newStatusId)) {
            throw new IllegalArgumentException(
                    String.format("Invalid status transition from %s to %s", currentStatusId, newStatusId));
        }
    }

    /**
     * Gets the human-readable status name for a status ID.
     *
     * @param statusId The status ID
     * @return The status name
     */
    private String getStatusName(String statusId) {
        return switch (statusId) {
            case "PENDING" -> "Pending";
            case "CONFIRMED" -> "Confirmed";
            case "PROCESSING" -> "Processing";
            case "SHIPPED" -> "Shipped";
            case "DELIVERED" -> "Delivered";
            case "CANCELLED" -> "Cancelled";
            default -> statusId;
        };
    }

    /**
     * Updates order dates based on the new status.
     *
     * @param order       The order to update
     * @param newStatusId The new status ID
     */
    private void updateOrderDatesForStatus(OrderHeader order, String newStatusId) {
        var now = Instant.now();
        switch (newStatusId) {
            case "SHIPPED" -> {
                if (order.getShippedDate() == null) {
                    order.setShippedDate(now);
                }
            }
            case "DELIVERED" -> {
                if (order.getDeliveredDate() == null) {
                    order.setDeliveredDate(now);
                }
            }
        }
    }

    /**
     * Validates that an order can be cancelled based on business rules.
     *
     * @param order The order to validate
     * @throws IllegalStateException if the order cannot be cancelled
     */
    private void validateOrderCancellation(OrderHeader order) {
        var currentStatusId = order.getStatus() != null ? order.getStatus().getId() : "PENDING";
        
        // Orders that are already shipped or delivered cannot be cancelled
        if ("SHIPPED".equals(currentStatusId) || "DELIVERED".equals(currentStatusId)) {
            throw new IllegalStateException(
                    String.format("Cannot cancel order in status %s", currentStatusId));
        }
        
        // Orders that are already cancelled cannot be cancelled again
        if ("CANCELLED".equals(currentStatusId)) {
            throw new IllegalStateException("Order is already cancelled");
        }
        
        // Additional validation based on shipped date (as a safety check)
        if (order.getShippedDate() != null) {
            throw new IllegalStateException("Cannot cancel order that has already been shipped");
        }
    }

    /**
     * Reduces inventory stock for an order item during direct order creation.
     *
     * @param storeId     The store ID
     * @param itemDto     The order item DTO
     * @param orderNumber The order number for reference
     */
    private void reduceInventoryForOrderItem(UUID storeId, CreateOrderDto.CreateOrderItemDto itemDto, String orderNumber) {
        try {
            var adjustmentDto = new InventoryAdjustmentDto(
                    itemDto.productId(),
                    InventoryAdjustmentDto.AdjustmentType.DECREASE,  // Remove from stock
                    itemDto.quantity(),  // Quantity to reduce
                    String.format("Direct order creation: %s", orderNumber),
                    orderNumber,  // Reference to order
                    null   // No additional notes
            );
            inventoryService.recordInventoryAdjustment(storeId, adjustmentDto);
            Log.infof("Reduced %d units of product %s from inventory for order %s",
                    itemDto.quantity(), itemDto.productId(), orderNumber);
        } catch (Exception e) {
            Log.warnf("Failed to reduce inventory for product %s in order %s: %s",
                    itemDto.productId(), orderNumber, e.getMessage());
            // In production, you might want to throw an exception here to prevent overselling
            // throw new IllegalStateException("Failed to reduce inventory for product: " + itemDto.productId(), e);
        }
    }

    /**
     * Restores inventory for all items in a cancelled order.
     *
     * @param storeId The store ID
     * @param order   The cancelled order
     */
    private void restoreInventoryForCancelledOrder(UUID storeId, OrderHeader order) {
        var orderItems = orderItemRepository.findByOrderId(order.getId());
        
        for (var orderItem : orderItems) {
            try {
                var productId = orderItem.getProduct().getId();
                var quantity = orderItem.getQuantity();
                var reason = String.format("Order cancellation: %s", order.getOrderNumber());
                
                var adjustmentDto = new InventoryAdjustmentDto(
                        productId,
                        InventoryAdjustmentDto.AdjustmentType.INCREASE,  // Add back to stock
                        quantity,  // Positive quantity to add back to stock
                        reason,
                        null,  // No reference
                        null   // No additional notes
                );
                inventoryService.recordInventoryAdjustment(storeId, adjustmentDto);
                Log.infof("Restored %d units of product %s to inventory for cancelled order %s",
                        quantity, orderItem.getProductSku(), order.getOrderNumber());
            } catch (Exception e) {
                // Log but don't fail the cancellation if inventory restoration fails
                Log.warnf("Failed to restore inventory for product %s in cancelled order %s: %s",
                        orderItem.getProductSku(), order.getOrderNumber(), e.getMessage());
            }
        }
    }

    // Refund functionality methods

    @Override
    @Transactional
    public RefundDto createRefund(UUID storeId, CreateRefundDto createRefundDto) {
        Log.infof("Creating refund for order %s in store %s (type: %s, amount: %s)",
                createRefundDto.orderId(), storeId, createRefundDto.refundType(), createRefundDto.refundAmount());

        // Find the order
        var order = orderRepository.findByIdOptional(createRefundDto.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + createRefundDto.orderId()));

        // Validate order is refundable
        validateOrderRefundable(order);

        // Calculate refund amount and validate
        var calculatedRefundAmount = calculateRefundAmount(order, createRefundDto);
        var remainingRefundableAmount = getRemainingRefundableAmount(order);
        
        if (calculatedRefundAmount.compareTo(remainingRefundableAmount) > 0) {
            throw new IllegalStateException(
                    String.format("Refund amount %.2f exceeds remaining refundable amount %.2f",
                            calculatedRefundAmount.doubleValue(), remainingRefundableAmount.doubleValue()));
        }

        // Generate refund number
        var refundNumber = generateRefundNumber();

        // Create refund items
        var refundItems = createRefundItems(order, createRefundDto);

        // In a real implementation, this would create and persist Refund and RefundItem entities
        // For now, we'll create a DTO with simulated data
        var refundId = UUID.randomUUID();
        var now = Instant.now();
        
        var refund = new RefundDto(
                refundId,
                order.getId(),
                order.getOrderNumber(),
                refundNumber,
                createRefundDto.refundType().name(),
                "PENDING", // Initial status
                calculatedRefundAmount,
                null, // Not yet processed
                createRefundDto.reason(),
                createRefundDto.refundMethod(),
                refundItems,
                createRefundDto.notes(),
                now, // Requested date
                null, // Not yet processed
                now, // Created at
                now, // Updated at
                1L   // Version
        );

        Log.infof("Created refund %s for order %s (amount: %.2f)", 
                refundNumber, order.getOrderNumber(), calculatedRefundAmount.doubleValue());
        
        return refund;
    }

    @Override
    public List<RefundDto> getOrderRefunds(UUID storeId, UUID orderId) {
        Log.infof("Getting refunds for order %s in store %s", orderId, storeId);
        
        // In a real implementation, this would query refund repository
        // For now, return empty list as we don't have persistent refunds
        return List.of();
    }

    @Override
    public Optional<RefundDto> findRefund(UUID storeId, UUID refundId) {
        Log.infof("Finding refund %s in store %s", refundId, storeId);
        
        // In a real implementation, this would query refund repository
        // For now, return empty as we don't have persistent refunds
        return Optional.empty();
    }

    @Override
    @Transactional
    public Optional<RefundDto> processRefund(UUID storeId, UUID refundId, BigDecimal processedAmount, String notes) {
        Log.infof("Processing refund %s in store %s (amount: %.2f)", 
                refundId, storeId, processedAmount.doubleValue());
        
        // In a real implementation, this would:
        // 1. Find the refund by ID
        // 2. Validate it's in PENDING status
        // 3. Process the payment refund via payment gateway
        // 4. Update refund status to PROCESSED
        // 5. Set processed amount and date
        // 6. Update inventory if needed
        
        // For now, return empty as we don't have persistent refunds
        return Optional.empty();
    }

    // Helper methods for refund functionality

    /**
     * Validates that an order can be refunded.
     *
     * @param order The order to validate
     * @throws IllegalStateException if the order cannot be refunded
     */
    private void validateOrderRefundable(OrderHeader order) {
        var currentStatusId = order.getStatus() != null ? order.getStatus().getId() : "PENDING";
        
        // Only orders that are DELIVERED can be refunded (in most business models)
        // CANCELLED orders can also be refunded if payment was already processed
        if (!"DELIVERED".equals(currentStatusId) && !"CANCELLED".equals(currentStatusId)) {
            throw new IllegalStateException(
                    String.format("Order in status %s cannot be refunded", currentStatusId));
        }
        
        // Additional validation could include checking refund time limits
        if (order.getDeliveredDate() != null) {
            var daysSinceDelivery = java.time.Duration.between(order.getDeliveredDate(), Instant.now()).toDays();
            if (daysSinceDelivery > 30) {
                throw new IllegalStateException("Refund period has expired (30 days after delivery)");
            }
        }
    }

    /**
     * Calculates the refund amount based on the refund request.
     *
     * @param order           The order being refunded
     * @param createRefundDto The refund request
     * @return The calculated refund amount
     */
    private BigDecimal calculateRefundAmount(OrderHeader order, CreateRefundDto createRefundDto) {
        if (createRefundDto.refundType() == CreateRefundDto.RefundType.FULL) {
            // Full refund - return total amount paid
            return order.getTotalAmount();
        }
        
        if (createRefundDto.refundAmount() != null) {
            // Explicit refund amount specified
            return createRefundDto.refundAmount();
        }
        
        if (createRefundDto.items() != null && !createRefundDto.items().isEmpty()) {
            // Calculate based on items being refunded
            return createRefundDto.items().stream()
                    .map(item -> {
                        if (item.refundAmount() != null) {
                            return item.refundAmount();
                        }
                        // Find the original order item to calculate proportional refund
                        var orderItems = orderItemRepository.findByOrderId(order.getId());
                        var originalItem = orderItems.stream()
                                .filter(oi -> oi.getId().equals(item.orderItemId()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + item.orderItemId()));
                        
                        // Calculate proportional amount based on quantity
                        var unitRefund = originalItem.getTotalPrice().divide(BigDecimal.valueOf(originalItem.getQuantity()));
                        return unitRefund.multiply(BigDecimal.valueOf(item.quantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        throw new IllegalArgumentException("Cannot determine refund amount from request");
    }

    /**
     * Gets the remaining refundable amount for an order.
     *
     * @param order The order
     * @return The remaining amount that can be refunded
     */
    private BigDecimal getRemainingRefundableAmount(OrderHeader order) {
        // In a real implementation, this would subtract already processed refunds
        // For now, assume the full order amount is refundable
        return order.getTotalAmount();
    }

    /**
     * Creates refund item DTOs from the refund request.
     *
     * @param order           The order being refunded
     * @param createRefundDto The refund request
     * @return List of refund item DTOs
     */
    private List<RefundDto.RefundItemDto> createRefundItems(OrderHeader order, CreateRefundDto createRefundDto) {
        if (createRefundDto.refundType() == CreateRefundDto.RefundType.FULL) {
            // Full refund - include all order items
            return orderItemRepository.findByOrderId(order.getId()).stream()
                    .map(orderItem -> new RefundDto.RefundItemDto(
                            UUID.randomUUID(), // Refund item ID
                            UUID.randomUUID(), // Refund ID (would be actual refund ID)
                            orderItem.getId(),
                            orderItem.getProduct().getId(),
                            orderItem.getProductName(),
                            orderItem.getProductSku(),
                            orderItem.getQuantity(),
                            orderItem.getUnitPrice(),
                            orderItem.getTotalPrice(),
                            Instant.now(),
                            Instant.now(),
                            1L
                    ))
                    .toList();
        }
        
        if (createRefundDto.items() != null) {
            // Partial refund - include specified items
            var orderItems = orderItemRepository.findByOrderId(order.getId());
            return createRefundDto.items().stream()
                    .map(refundItem -> {
                        var orderItem = orderItems.stream()
                                .filter(oi -> oi.getId().equals(refundItem.orderItemId()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + refundItem.orderItemId()));
                        
                        var refundAmount = refundItem.refundAmount() != null ? 
                                refundItem.refundAmount() :
                                orderItem.getUnitPrice().multiply(BigDecimal.valueOf(refundItem.quantity()));
                        
                        return new RefundDto.RefundItemDto(
                                UUID.randomUUID(), // Refund item ID
                                UUID.randomUUID(), // Refund ID (would be actual refund ID)
                                orderItem.getId(),
                                orderItem.getProduct().getId(),
                                orderItem.getProductName(),
                                orderItem.getProductSku(),
                                refundItem.quantity(),
                                orderItem.getUnitPrice(),
                                refundAmount,
                                Instant.now(),
                                Instant.now(),
                                1L
                        );
                    })
                    .toList();
        }
        
        // No items specified - empty list
        return List.of();
    }

    /**
     * Generates a unique refund number.
     *
     * @return A unique refund number
     */
    private String generateRefundNumber() {
        var now = java.time.LocalDateTime.now();
        var datePrefix = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        var uniqueSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("REF-%s-%s", datePrefix, uniqueSuffix);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
