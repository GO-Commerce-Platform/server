package dev.tiodati.saas.gocommerce.order.service;

import dev.tiodati.saas.gocommerce.inventory.dto.InventoryAdjustmentDto;
import dev.tiodati.saas.gocommerce.inventory.service.InventoryService;
import dev.tiodati.saas.gocommerce.order.entity.OrderHeader;
import dev.tiodati.saas.gocommerce.order.entity.OrderItem;
import dev.tiodati.saas.gocommerce.order.entity.OrderStatus;
import dev.tiodati.saas.gocommerce.order.repository.OrderItemRepository;
import dev.tiodati.saas.gocommerce.order.repository.OrderRepository;
import dev.tiodati.saas.gocommerce.product.entity.Product;
import dev.tiodati.saas.gocommerce.cart.repository.ShoppingCartRepository;
import dev.tiodati.saas.gocommerce.cart.repository.CartItemRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

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
}
