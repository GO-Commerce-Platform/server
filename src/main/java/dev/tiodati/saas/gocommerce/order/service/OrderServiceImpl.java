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
import dev.tiodati.saas.gocommerce.order.dto.OrderDto;
import dev.tiodati.saas.gocommerce.order.dto.OrderItemDto;
import dev.tiodati.saas.gocommerce.order.entity.OrderHeader;
import dev.tiodati.saas.gocommerce.order.entity.OrderItem;
import dev.tiodati.saas.gocommerce.order.entity.OrderStatus;
import dev.tiodati.saas.gocommerce.order.repository.OrderRepository;
import dev.tiodati.saas.gocommerce.product.entity.Product;

/**
 * Implementation of OrderService providing store-specific order management
 * functionality.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /**
     * Repository for order data access operations.
     */
    private final OrderRepository orderRepository;

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
        // Get order items (this would require a separate query or lazy loading)
        List<OrderItemDto> items = List.of(); // FIXME: Implement order items loading

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
     * Creates an OrderItem entity from the create DTO.
     *
     * @param order   The parent order
     * @param itemDto The item creation DTO
     * @return The created OrderItem entity
     */
    private OrderItem createOrderItem(OrderHeader order, CreateOrderDto.CreateOrderItemDto itemDto) {
        // Create a product reference using EntityManager.getReference to avoid detached entity issues
        var product = orderRepository.getEntityManager().getReference(Product.class, itemDto.productId());

        var totalPrice = itemDto.unitPrice().multiply(BigDecimal.valueOf(itemDto.quantity()));

        return OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(itemDto.quantity())
                .unitPrice(itemDto.unitPrice())
                .totalPrice(totalPrice)
                .productName("Product Name") // FIXME: Fetch from product
                .productSku("SKU") // FIXME: Fetch from product
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
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
