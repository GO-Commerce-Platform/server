package dev.tiodati.saas.gocommerce.order.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.order.dto.CreateOrderDto;
import dev.tiodati.saas.gocommerce.order.repository.OrderRepository;
import dev.tiodati.saas.gocommerce.order.service.OrderService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Integration tests for OrderService implementation.
 * Tests the complete order workflow using real service and database
 * interactions.
 */
@QuarkusTest
@DisplayName("OrderServiceImpl Tests")
class OrderServiceImplTest {

    @Inject
    private OrderService orderService;

    @Inject
    private OrderRepository orderRepository;

    private UUID storeId;
    private UUID customerId;
    private CreateOrderDto createOrderDto;

    @BeforeEach
    void setUp() {
        // Generate unique test data for isolation
        var uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        storeId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        // Create test order items
        var orderItem = new CreateOrderDto.CreateOrderItemDto(
                UUID.randomUUID(),
                2,
                new BigDecimal("49.99"));

        createOrderDto = new CreateOrderDto(
                customerId,
                "John",
                "Doe",
                "123 Main St",
                "Apt 4B",
                "New York",
                "NY",
                "10001",
                "US",
                "+1234567890",
                "John",
                "Doe",
                "123 Main St",
                "Apt 4B",
                "New York",
                "NY",
                "10001",
                "US",
                "+1234567890",
                "Test order - " + uniqueSuffix,
                List.of(orderItem));
    }

    @AfterEach
    void tearDown() {
        // Clean up test data to maintain test isolation
        orderRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully")
        void shouldCreateOrderSuccessfully() {
            // When - Real service call
            var result = orderService.createOrder(storeId, createOrderDto);

            // Then - Assert real results
            assertNotNull(result);
            assertNotNull(result.id());
            assertNotNull(result.orderNumber());
            assertEquals(createOrderDto.customerId(), result.customerId());
            assertEquals("PENDING", result.statusId());
            assertNotNull(result.orderDate());
            assertTrue(result.totalAmount().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should calculate totals correctly")
        void shouldCalculateTotalsCorrectly() {
            // When - Real service call
            var result = orderService.createOrder(storeId, createOrderDto);

            // Then - Assert calculation is correct
            // Expected: 2 items * $49.99 = $99.98 subtotal
            var expectedSubtotal = new BigDecimal("99.98");
            assertEquals(0, expectedSubtotal.compareTo(result.subtotal()));

            // Total should include tax and shipping
            assertTrue(result.totalAmount().compareTo(result.subtotal()) >= 0);
        }
    }

    @Nested
    @DisplayName("List Orders Tests")
    class ListOrdersTests {

        @Test
        @DisplayName("Should list orders with pagination")
        void shouldListOrdersWithPagination() {
            // Given - Create real test data
            var order = orderService.createOrder(storeId, createOrderDto);

            // When - Real service call
            var result = orderService.listOrders(storeId, 0, 10, null);

            // Then - Assert real results
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(order.id(), result.get(0).id());
        }

        @Test
        @DisplayName("Should filter orders by status")
        void shouldFilterOrdersByStatus() {
            // Given - Create real test data
            var order = orderService.createOrder(storeId, createOrderDto);

            // When - Real service call filtering by PENDING status
            var result = orderService.listOrders(storeId, 0, 10, "PENDING");

            // Then - Assert real results
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(order.id(), result.get(0).id());
            assertEquals("PENDING", result.get(0).statusId());
        }

        @Test
        @DisplayName("Should return empty list when no orders found")
        void shouldReturnEmptyListWhenNoOrdersFound() {
            // When - Real service call without creating any orders
            var result = orderService.listOrders(storeId, 0, 10, null);

            // Then - Assert empty result
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find Order Tests")
    class FindOrderTests {

        @Test
        @DisplayName("Should find order by ID")
        void shouldFindOrderById() {
            // Given - Create real order
            var order = orderService.createOrder(storeId, createOrderDto);

            // When - Real service call
            var result = orderService.findOrder(storeId, order.id());

            // Then - Assert real results
            assertTrue(result.isPresent());
            assertEquals(order.id(), result.get().id());
            assertEquals(order.orderNumber(), result.get().orderNumber());
        }

        @Test
        @DisplayName("Should return empty when order not found")
        void shouldReturnEmptyWhenOrderNotFound() {
            // Given - Non-existent order ID
            var nonExistentId = UUID.randomUUID();

            // When - Real service call
            var result = orderService.findOrder(storeId, nonExistentId);

            // Then - Assert empty result
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should find order by order number")
        void shouldFindOrderByOrderNumber() {
            // Given - Create real order
            var order = orderService.createOrder(storeId, createOrderDto);

            // When - Real service call
            var result = orderService.findOrderByNumber(storeId, order.orderNumber());

            // Then - Assert real results
            assertTrue(result.isPresent());
            assertEquals(order.id(), result.get().id());
            assertEquals(order.orderNumber(), result.get().orderNumber());
        }
    }

    @Nested
    @DisplayName("Update Order Status Tests")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Should update order status successfully")
        void shouldUpdateOrderStatusSuccessfully() {
            // Given - Create real order
            var order = orderService.createOrder(storeId, createOrderDto);

            // When - Real service call to update to PROCESSING status
            var result = orderService.updateOrderStatus(storeId, order.id(), "PROCESSING");

            // Then - Assert real results
            assertTrue(result.isPresent());
            assertEquals("PROCESSING", result.get().statusId());
        }

        @Test
        @DisplayName("Should return empty when updating non-existent order")
        void shouldReturnEmptyWhenUpdatingNonExistentOrder() {
            // Given - Non-existent order ID
            var nonExistentId = UUID.randomUUID();

            // When - Real service call
            var result = orderService.updateOrderStatus(storeId, nonExistentId, "PROCESSING");

            // Then - Assert empty result
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Calculate Order Total Tests")
    class CalculateOrderTotalTests {

        @Test
        @DisplayName("Should calculate order totals correctly")
        void shouldCalculateOrderTotalsCorrectly() {
            // Given - Create multiple order items
            var orderItem1 = new CreateOrderDto.CreateOrderItemDto(
                    UUID.randomUUID(),
                    1,
                    new BigDecimal("25.00"));
            var orderItem2 = new CreateOrderDto.CreateOrderItemDto(
                    UUID.randomUUID(),
                    3,
                    new BigDecimal("15.50"));

            var multiItemOrderDto = new CreateOrderDto(
                    customerId,
                    "Jane",
                    "Smith",
                    "456 Oak Ave",
                    null,
                    "Boston",
                    "MA",
                    "02101",
                    "US",
                    "+9876543210",
                    "Jane",
                    "Smith",
                    "456 Oak Ave",
                    null,
                    "Boston",
                    "MA",
                    "02101",
                    "US",
                    "+9876543210",
                    "Multi-item test order",
                    List.of(orderItem1, orderItem2));

            // When - Real service call
            var result = orderService.createOrder(storeId, multiItemOrderDto);

            // Then - Assert totals calculation
            // Expected: (1 * $25.00) + (3 * $15.50) = $25.00 + $46.50 = $71.50
            var expectedSubtotal = new BigDecimal("71.50");
            assertEquals(0, expectedSubtotal.compareTo(result.subtotal()));

            // Verify total amount includes subtotal
            assertTrue(result.totalAmount().compareTo(result.subtotal()) >= 0);
            assertNotNull(result.taxAmount());
            assertNotNull(result.shippingAmount());
        }
    }

    @Nested
    @DisplayName("Count Orders Tests")
    class CountOrdersTests {

        @Test
        @DisplayName("Should count orders by status")
        void shouldCountOrdersByStatus() {
            // Given - Create real test data
            orderService.createOrder(storeId, createOrderDto);

            // When - Real service call
            var result = orderService.countOrdersByStatus(storeId, "PENDING");

            // Then - Assert real results
            assertEquals(1L, result);
        }

        @Test
        @DisplayName("Should return zero when no orders with status")
        void shouldReturnZeroWhenNoOrdersWithStatus() {
            // When - Real service call without creating any orders
            var result = orderService.countOrdersByStatus(storeId, "CANCELLED");

            // Then - Assert empty result
            assertEquals(0L, result);
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel order successfully")
        void shouldCancelOrderSuccessfully() {
            // Given - Create real order that hasn't been shipped
            var order = orderService.createOrder(storeId, createOrderDto);
            var reason = "Customer requested cancellation";

            // When - Real service call
            var result = orderService.cancelOrder(storeId, order.id(), reason);

            // Then - Assert real results
            assertTrue(result.isPresent());
            assertEquals("CANCELLED", result.get().statusId());
            assertTrue(result.get().notes().contains("Cancelled: " + reason));
        }

        @Test
        @DisplayName("Should throw exception when cancelling shipped order")
        void shouldThrowExceptionWhenCancellingShippedOrder() {
            // Given - Create real order and mark it as shipped
            var order = orderService.createOrder(storeId, createOrderDto);

            // Update the order to set shipped date directly in repository
            var orderEntity = orderRepository.findByIdOptional(order.id()).orElseThrow();
            orderEntity.setShippedDate(Instant.now());
            orderRepository.persist(orderEntity);

            var reason = "Customer requested cancellation";

            // When & Then - Should throw exception for shipped order
            assertThrows(IllegalStateException.class,
                    () -> orderService.cancelOrder(storeId, order.id(), reason));
        }

        @Test
        @DisplayName("Should return empty when cancelling non-existent order")
        void shouldReturnEmptyWhenCancellingNonExistentOrder() {
            // Given - Non-existent order ID
            var nonExistentId = UUID.randomUUID();
            var reason = "Customer requested cancellation";

            // When - Real service call
            var result = orderService.cancelOrder(storeId, nonExistentId, reason);

            // Then - Assert empty result
            assertFalse(result.isPresent());
        }
    }

}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
