package dev.tiodati.saas.gocommerce.order.service;

import dev.tiodati.saas.gocommerce.inventory.dto.InventoryAdjustmentDto;
import dev.tiodati.saas.gocommerce.inventory.service.InventoryService;
import dev.tiodati.saas.gocommerce.order.dto.CreateOrderDto;
import dev.tiodati.saas.gocommerce.order.entity.OrderHeader;
import dev.tiodati.saas.gocommerce.order.entity.OrderItem;
import dev.tiodati.saas.gocommerce.order.entity.OrderStatus;
import dev.tiodati.saas.gocommerce.order.repository.OrderItemRepository;
import dev.tiodati.saas.gocommerce.order.repository.OrderRepository;
import dev.tiodati.saas.gocommerce.product.entity.Product;
import dev.tiodati.saas.gocommerce.cart.repository.ShoppingCartRepository;
import dev.tiodati.saas.gocommerce.cart.repository.CartItemRepository;
import dev.tiodati.saas.gocommerce.promotion.service.PromotionService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class OrderServiceImplTest {

    @Inject
    OrderServiceImpl orderService;

    @InjectMock
    OrderRepository orderRepository;

    @InjectMock
    OrderItemRepository orderItemRepository;

    @InjectMock
    ShoppingCartRepository cartRepository;

    @InjectMock
    CartItemRepository cartItemRepository;

    @InjectMock
    InventoryService inventoryService;

    @InjectMock
    PromotionService promotionService;

    private OrderHeader testOrder;
    private OrderItem testOrderItem;
    private Product testProduct;
    private UUID storeId;
    private UUID orderId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        productId = UUID.randomUUID();

        // Create test product
        testProduct = new Product();
        testProduct.setId(productId);
        testProduct.setName("Test Product");
        testProduct.setSku("TEST-SKU-001");

        // Create test order status
        var orderStatus = new OrderStatus();
        orderStatus.setId("PENDING");
        orderStatus.setName("Pending");

        // Create test order
        testOrder = new OrderHeader();
        testOrder.setId(orderId);
        testOrder.setOrderNumber("ORD-TEST-001");
        testOrder.setStatus(orderStatus);

        // Create test order item
        testOrderItem = new OrderItem();
        testOrderItem.setId(UUID.randomUUID());
        testOrderItem.setOrder(testOrder);
        testOrderItem.setProduct(testProduct);
        testOrderItem.setQuantity(2);
        testOrderItem.setProductSku("TEST-SKU-001");
    }

    @Test
    @DisplayName("Should update order status with valid transition")
    void testUpdateOrderStatus_ValidTransition() {
        // Given
        when(orderRepository.findByIdOptional(orderId)).thenReturn(Optional.of(testOrder));
        // Mock persist - it returns void so we just verify it gets called

        // When
        var result = orderService.updateOrderStatus(storeId, orderId, "CONFIRMED");

        // Then
        assertTrue(result.isPresent());
        assertEquals("CONFIRMED", testOrder.getStatus().getId());
        assertEquals("Confirmed", testOrder.getStatus().getName());
        verify(orderRepository).persist(testOrder);
    }

    @Test
    @DisplayName("Should reject invalid status transition")
    void testUpdateOrderStatus_InvalidTransition() {
        // Given
        testOrder.getStatus().setId("DELIVERED");
        when(orderRepository.findByIdOptional(orderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.updateOrderStatus(storeId, orderId, "PENDING");
        });
    }

    @Test
    @DisplayName("Should update shipped date when status changes to SHIPPED")
    void testUpdateOrderStatus_UpdatesShippedDate() {
        // Given
        when(orderRepository.findByIdOptional(orderId)).thenReturn(Optional.of(testOrder));
        // Mock persist - it returns void so we just verify it gets called

        // When
        orderService.updateOrderStatus(storeId, orderId, "CONFIRMED");
        orderService.updateOrderStatus(storeId, orderId, "PROCESSING");
        orderService.updateOrderStatus(storeId, orderId, "SHIPPED");

        // Then
        assertNotNull(testOrder.getShippedDate());
    }

    @Test
    @DisplayName("Should cancel order and restore inventory")
    void testCancelOrder_Success() {
        // Given
        when(orderRepository.findByIdOptional(orderId)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(testOrderItem));
        // Mock persist - it returns void so we just verify it gets called

        // When
        var result = orderService.cancelOrder(storeId, orderId, "Customer requested cancellation");

        // Then
        assertTrue(result.isPresent());
        assertEquals("CANCELLED", testOrder.getStatus().getId());
        assertTrue(testOrder.getNotes().contains("Cancelled: Customer requested cancellation"));

        // Verify inventory restoration
        ArgumentCaptor<InventoryAdjustmentDto> adjustmentCaptor = 
            ArgumentCaptor.forClass(InventoryAdjustmentDto.class);
        verify(inventoryService).recordInventoryAdjustment(eq(storeId), adjustmentCaptor.capture());

        var adjustment = adjustmentCaptor.getValue();
        assertEquals(productId, adjustment.productId());
        assertEquals(InventoryAdjustmentDto.AdjustmentType.INCREASE, adjustment.adjustmentType());
        assertEquals(2, adjustment.quantity()); // Same as order item quantity
        assertTrue(adjustment.reason().contains("Order cancellation"));
    }

    @Test
    @DisplayName("Should prevent cancellation of shipped order")
    void testCancelOrder_PreventShippedOrderCancellation() {
        // Given
        testOrder.getStatus().setId("SHIPPED");
        testOrder.setShippedDate(Instant.now());
        when(orderRepository.findByIdOptional(orderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(storeId, orderId, "Should not work");
        });
    }

    @Test
    @DisplayName("Should prevent cancellation of already cancelled order")
    void testCancelOrder_PreventAlreadyCancelledOrder() {
        // Given
        testOrder.getStatus().setId("CANCELLED");
        when(orderRepository.findByIdOptional(orderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(storeId, orderId, "Should not work");
        });
    }

    @Test
    @DisplayName("Should handle inventory restoration failure gracefully")
    void testCancelOrder_InventoryRestorationFailure() {
        // Given
        when(orderRepository.findByIdOptional(orderId)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(testOrderItem));
        // Mock persist - it returns void so we just verify it gets called
        doThrow(new RuntimeException("Inventory service unavailable"))
            .when(inventoryService).recordInventoryAdjustment(eq(storeId), any());

        // When
        var result = orderService.cancelOrder(storeId, orderId, "Test cancellation");

        // Then - Order should still be cancelled even if inventory restoration fails
        assertTrue(result.isPresent());
        assertEquals("CANCELLED", testOrder.getStatus().getId());
        verify(orderRepository).persist(testOrder);
    }

    @Test
    @DisplayName("Should mark order as shipped with correct date")
    void testMarkOrderShipped() {
        // Given - Set order to PROCESSING status (valid transition to SHIPPED)
        var processingStatus = new OrderStatus();
        processingStatus.setId("PROCESSING");
        processingStatus.setName("Processing");
        testOrder.setStatus(processingStatus);
        
        var shippedDate = Instant.now();
        when(orderRepository.findByIdOptional(orderId)).thenReturn(Optional.of(testOrder));
        // Mock persist - it returns void so we just verify it gets called

        // When
        var result = orderService.markOrderShipped(storeId, orderId, shippedDate);

        // Then
        assertTrue(result.isPresent());
        assertEquals(shippedDate, testOrder.getShippedDate());
        assertEquals("SHIPPED", testOrder.getStatus().getId());
    }

    @Test
    @DisplayName("Should mark order as delivered with correct date")
    void testMarkOrderDelivered() {
        // Given - Set order to SHIPPED status (valid transition to DELIVERED)
        var shippedStatus = new OrderStatus();
        shippedStatus.setId("SHIPPED");
        shippedStatus.setName("Shipped");
        testOrder.setStatus(shippedStatus);
        
        var deliveredDate = Instant.now();
        when(orderRepository.findByIdOptional(orderId)).thenReturn(Optional.of(testOrder));
        // Mock persist - it returns void so we just verify it gets called

        // When
        var result = orderService.markOrderDelivered(storeId, orderId, deliveredDate);

        // Then
        assertTrue(result.isPresent());
        assertEquals(deliveredDate, testOrder.getDeliveredDate());
        assertEquals("DELIVERED", testOrder.getStatus().getId());
    }

    @Test
    @DisplayName("Should count orders by status")
    void testCountOrdersByStatus() {
        // Given
        when(orderRepository.countByStatus(any(OrderStatus.class))).thenReturn(5L);

        // When
        var count = orderService.countOrdersByStatus(storeId, "PENDING");

        // Then
        assertEquals(5L, count);
        verify(orderRepository).countByStatus(any(OrderStatus.class));
    }

    @Test
    @DisplayName("Should return correct status name for status ID")
    void testGetStatusName() {
        // Use reflection or create a public test method to access private method
        // For now, we'll test this indirectly through updateOrderStatus
        when(orderRepository.findByIdOptional(orderId)).thenReturn(Optional.of(testOrder));
        // Mock persist - it returns void so we just verify it gets called

        // First go through valid transition sequence: PENDING -> CONFIRMED -> PROCESSING
        orderService.updateOrderStatus(storeId, orderId, "CONFIRMED");
        orderService.updateOrderStatus(storeId, orderId, "PROCESSING");

        assertEquals("Processing", testOrder.getStatus().getName());
    }

    @Test
    @DisplayName("Should create order with promotion discount applied")
    void testCreateOrder_WithPromotionDiscount() {
        // Given
        var customerId = UUID.randomUUID();
        var orderItemDto = new CreateOrderDto.CreateOrderItemDto(
                productId, 2, new BigDecimal("50.00")
        );
        var createOrderDto = new CreateOrderDto(
                customerId,
                "Test", "Customer",
                "123 Main St", null,
                "Test City", "CA", "12345", "USA", "555-1234",
                "Test", "Customer",
                "123 Main St", null,
                "Test City", "CA", "12345", "USA", "555-1234",
                "Applied code SAVE10", // Notes containing promotion code
                List.of(orderItemDto)
        );

        // Mock promotion service to return 10% discount ($10.00 on $100 order)
        when(promotionService.calculateBestDiscount(eq(storeId), eq(new BigDecimal("100.00")), any()))
                .thenReturn(new BigDecimal("10.00"));
        
        // Mock inventory service
        when(inventoryService.recordInventoryAdjustment(eq(storeId), any())).thenReturn(true);
        
        // Mock entity manager to return the product
        var entityManager = mock(jakarta.persistence.EntityManager.class);
        when(orderRepository.getEntityManager()).thenReturn(entityManager);
        when(entityManager.find(Product.class, productId)).thenReturn(testProduct);
        doNothing().when(entityManager).persist(any());

        // When
        var result = orderService.createOrder(storeId, createOrderDto);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.subtotal()); // 2 * $50.00
        assertEquals(new BigDecimal("10.00"), result.discountAmount()); // 10% discount
        // This line was incorrect, let me remove it
        // Total calculation: discountedSubtotal + tax + shipping
        // discountedSubtotal = subtotal - discount = 100 - 10 = 90
        // tax = discountedSubtotal * 0.10 = 90 * 0.10 = 9.00
        // shipping = 9.99 (flat rate)
        // total = 90 + 9.00 + 9.99 = 108.99
        assertEquals(new BigDecimal("108.99"), result.totalAmount());
        
        // Verify promotion service was called with correct parameters
        verify(promotionService).calculateBestDiscount(eq(storeId), eq(new BigDecimal("100.00")), argThat(codes -> 
                codes.contains("SAVE10")
        ));
    }

    @Test
    @DisplayName("Should create order with no discount when no promotion codes")
    void testCreateOrder_NoPromotionCodes() {
        // Given
        var customerId = UUID.randomUUID();
        var orderItemDto = new CreateOrderDto.CreateOrderItemDto(
                productId, 1, new BigDecimal("75.00")
        );
        var createOrderDto = new CreateOrderDto(
                customerId,
                "Test", "Customer",
                "123 Main St", null,
                "Test City", "CA", "12345", "USA", "555-1234",
                "Test", "Customer",
                "123 Main St", null,
                "Test City", "CA", "12345", "USA", "555-1234",
                "customer wants this asap", // Notes without promotion codes
                List.of(orderItemDto)
        );

        // Mock promotion service to return no discount
        when(promotionService.calculateBestDiscount(eq(storeId), eq(new BigDecimal("75.00")), any()))
                .thenReturn(BigDecimal.ZERO);
        
        // Mock inventory service
        when(inventoryService.recordInventoryAdjustment(eq(storeId), any())).thenReturn(true);
        
        // Mock entity manager to return the product
        var entityManager = mock(jakarta.persistence.EntityManager.class);
        when(orderRepository.getEntityManager()).thenReturn(entityManager);
        when(entityManager.find(Product.class, productId)).thenReturn(testProduct);
        doNothing().when(entityManager).persist(any());

        // When
        var result = orderService.createOrder(storeId, createOrderDto);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("75.00"), result.subtotal());
        assertEquals(BigDecimal.ZERO, result.discountAmount());
        
        // Verify promotion service was called with the extracted words (even though they're not real promo codes)
        // The promotion service should return zero discount for these non-matching codes
        verify(promotionService).calculateBestDiscount(eq(storeId), eq(new BigDecimal("75.00")), 
                argThat(codes -> codes.containsAll(List.of("CUSTOMER", "WANTS", "THIS", "ASAP"))));
    }

    @Test
    @DisplayName("Should handle promotion service failure gracefully")
    void testCreateOrder_PromotionServiceFailure() {
        // Given
        var customerId = UUID.randomUUID();
        var orderItemDto = new CreateOrderDto.CreateOrderItemDto(
                productId, 1, new BigDecimal("50.00")
        );
        var createOrderDto = new CreateOrderDto(
                customerId,
                "Test", "Customer",
                "123 Main St", null,
                "Test City", "CA", "12345", "USA", "555-1234",
                "Test", "Customer",
                "123 Main St", null,
                "Test City", "CA", "12345", "USA", "555-1234",
                "Applied code SAVE10",
                List.of(orderItemDto)
        );

        // Mock promotion service to throw exception
        when(promotionService.calculateBestDiscount(any(), any(), any()))
                .thenThrow(new RuntimeException("Promotion service unavailable"));
        
        // Mock inventory service
        when(inventoryService.recordInventoryAdjustment(eq(storeId), any())).thenReturn(true);
        
        // Mock entity manager to return the product
        var entityManager = mock(jakarta.persistence.EntityManager.class);
        when(orderRepository.getEntityManager()).thenReturn(entityManager);
        when(entityManager.find(Product.class, productId)).thenReturn(testProduct);
        doNothing().when(entityManager).persist(any());

        // When & Then - Order creation should fail gracefully
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(storeId, createOrderDto);
        });
    }

    @Test
    @DisplayName("Should extract promotion codes from order notes correctly")
    void testPromotionCodeExtraction() {
        // This tests the private method extractPromotionCodes indirectly
        // Given
        var customerId = UUID.randomUUID();
        var orderItemDto = new CreateOrderDto.CreateOrderItemDto(
                productId, 1, new BigDecimal("100.00")
        );
        var createOrderDto = new CreateOrderDto(
                customerId,
                "Test", "Customer",
                "123 Main St", null,
                "Test City", "CA", "12345", "USA", "555-1234",
                "Test", "Customer",
                "123 Main St", null,
                "Test City", "CA", "12345", "USA", "555-1234",
                "Please apply codes SAVE10 and VIP20 for extra discount!",
                List.of(orderItemDto)
        );

        // Mock promotion service
        when(promotionService.calculateBestDiscount(eq(storeId), eq(new BigDecimal("100.00")), any()))
                .thenReturn(new BigDecimal("15.00"));
        
        // Mock inventory service
        when(inventoryService.recordInventoryAdjustment(eq(storeId), any())).thenReturn(true);
        
        // Mock entity manager
        var entityManager = mock(jakarta.persistence.EntityManager.class);
        when(orderRepository.getEntityManager()).thenReturn(entityManager);
        when(entityManager.find(Product.class, productId)).thenReturn(testProduct);
        doNothing().when(entityManager).persist(any());

        // When
        var result = orderService.createOrder(storeId, createOrderDto);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("15.00"), result.discountAmount());
        
        // Verify promotion service was called with extracted codes
        verify(promotionService).calculateBestDiscount(eq(storeId), eq(new BigDecimal("100.00")), 
                argThat(codes -> codes.contains("SAVE10") && codes.contains("VIP20")));
    }
}
