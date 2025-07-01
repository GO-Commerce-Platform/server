package dev.tiodati.saas.gocommerce.cart.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.cart.dto.CreateCartItemDto;
import dev.tiodati.saas.gocommerce.cart.dto.UpdateCartItemDto;
import dev.tiodati.saas.gocommerce.cart.repository.ShoppingCartRepository;
import dev.tiodati.saas.gocommerce.cart.service.CartService;
import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import dev.tiodati.saas.gocommerce.customer.entity.Customer;
import dev.tiodati.saas.gocommerce.customer.repository.CustomerRepository;
import dev.tiodati.saas.gocommerce.customer.service.CustomerService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import dev.tiodati.saas.gocommerce.testinfra.MultiTenantTest;

/**
 * Integration tests for CartService implementation.
 * Tests the complete cart workflow including cart and item management.
 */
@QuarkusTest
@MultiTenantTest
class CartServiceImplTest {

    /**
     * Injected CartService instance for testing.
     */
    @Inject
    private CartService cartService;

    /**
     * Injected CustomerService for creating test customers.
     */
    @Inject
    private CustomerService customerService;

    /**
     * Injected CustomerRepository for cleanup.
     */
    @Inject
    private CustomerRepository customerRepository;

    /**
     * Injected ShoppingCartRepository for cleanup.
     */
    @Inject
    private ShoppingCartRepository cartRepository;

    private UUID storeId;
    private UUID customerId;
    private UUID customerId2;
    private CreateCustomerDto createCustomerDto;
    
    // This field will be injected by MultiTenantTestExtension if needed
    private String testSchemaName;

    @BeforeEach
    void setUp() {
        // Generate unique test data for isolation
        var uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        storeId = UUID.randomUUID();

        createCustomerDto = new CreateCustomerDto(
                "test-" + uniqueSuffix + "@example.com",
                "John",
                "Doe",
                "+1234567890",
                LocalDate.of(1990, 1, 1),
                Customer.Gender.MALE,
                "123 Main St",
                "Apt 4B",
                "New York",
                "NY",
                "10001",
                "US",
                false,
                "en");

        // Create test customers
        var customer = customerService.createCustomer(storeId, createCustomerDto);
        customerId = customer.id();

        // Create a second customer for tests that need multiple customers
        var createCustomerDto2 = new CreateCustomerDto(
                "test2-" + uniqueSuffix + "@example.com",
                "Jane",
                "Smith",
                "+1234567891",
                LocalDate.of(1992, 5, 15),
                Customer.Gender.FEMALE,
                "456 Oak Ave",
                "Unit 2A",
                "Boston",
                "MA",
                "02101",
                "US",
                false,
                "en");
        var customer2 = customerService.createCustomer(storeId, createCustomerDto2);
        customerId2 = customer2.id();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Clean up test data to maintain test isolation
        cartRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    @Transactional
    void testGetOrCreateCartForNewCustomer() {
        // Given - Use the pre-created customer
        // customerId is already set up in @BeforeEach

        // When
        var cartDto = cartService.getOrCreateCart(customerId);

        // Then
        assertNotNull(cartDto);
        assertNotNull(cartDto.id());
        assertEquals(customerId, cartDto.customerId());
        assertTrue(cartDto.isActive());
        assertNotNull(cartDto.expiresAt());
        assertTrue(cartDto.items().isEmpty());
        assertEquals(0, cartDto.totalItems());
        assertEquals(BigDecimal.ZERO, cartDto.totalAmount());
    }

    @Test
    @Transactional
    void testGetOrCreateCartForExistingCustomer() {
        // Given - Use the pre-created customer
        var firstCart = cartService.getOrCreateCart(customerId);

        // When
        var secondCart = cartService.getOrCreateCart(customerId);

        // Then
        assertEquals(firstCart.id(), secondCart.id());
        assertEquals(firstCart.customerId(), secondCart.customerId());
    }

    @Test
    @Transactional
    void testGetOrCreateGuestCart() {
        // Given
        var sessionId = "test-session-" + UUID.randomUUID().toString();

        // When
        var cartDto = cartService.getOrCreateGuestCart(sessionId);

        // Then
        assertNotNull(cartDto);
        assertNotNull(cartDto.id());
        assertEquals(sessionId, cartDto.sessionId());
        assertTrue(cartDto.isActive());
        assertNotNull(cartDto.expiresAt());
        assertTrue(cartDto.items().isEmpty());
    }

    @Test
    @Transactional
    void testGetCartById() {
        // Given - Use the pre-created customer
        var createdCart = cartService.getOrCreateCart(customerId);

        // When
        var retrievedCart = cartService.getCartById(createdCart.id());

        // Then
        assertTrue(retrievedCart.isPresent());
        assertEquals(createdCart.id(), retrievedCart.get().id());
        assertEquals(createdCart.customerId(), retrievedCart.get().customerId());
    }

    @Test
    @Transactional
    void testGetCartByIdNotFound() {
        // Given
        var nonExistentCartId = UUID.randomUUID();

        // When
        var result = cartService.getCartById(nonExistentCartId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testAddItemToCartThrowsExceptionForNonExistentCart() {
        // Given
        var nonExistentCartId = UUID.randomUUID();
        var createItemDto = new CreateCartItemDto(UUID.randomUUID(), 1);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            cartService.addItemToCart(nonExistentCartId, createItemDto);
        });
    }

    @Test
    @Transactional
    void testUpdateCartItemThrowsExceptionForNonExistentItem() {
        // Given - Use the pre-created customer
        var cart = cartService.getOrCreateCart(customerId);
        var nonExistentItemId = UUID.randomUUID();
        var updateItemDto = new UpdateCartItemDto(5);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            cartService.updateCartItem(cart.id(), nonExistentItemId, updateItemDto);
        });
    }

    @Test
    @Transactional
    void testRemoveItemFromCartThrowsExceptionForNonExistentItem() {
        // Given - Use the pre-created customer
        var cart = cartService.getOrCreateCart(customerId);
        var nonExistentItemId = UUID.randomUUID();

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            cartService.removeItemFromCart(cart.id(), nonExistentItemId);
        });
    }

    @Test
    @Transactional
    void testClearCart() {
        // Given - Use the pre-created customer
        var cart = cartService.getOrCreateCart(customerId);

        // When
        cartService.clearCart(cart.id());

        // Then
        var clearedCart = cartService.getCartById(cart.id());
        assertTrue(clearedCart.isPresent());
        assertTrue(clearedCart.get().items().isEmpty());
        assertEquals(0, clearedCart.get().totalItems());
        assertEquals(BigDecimal.ZERO, clearedCart.get().totalAmount());
    }

    @Test
    void testDeactivateCart() {
        // Given - Use the pre-created customer
        var cart = cartService.getOrCreateCart(customerId);
        var cartId = cart.id();

        // When
        cartService.deactivateCart(cartId);

        // Then
        assertFalse(cartService.isCartActive(cartId));

        // Deactivated cart should not be returned by getCartById
        var retrievedCart = cartService.getCartById(cartId);
        assertFalse(retrievedCart.isPresent());
    }

    @Test
    void testIsCartActive() {
        // Given - Use the pre-created customer
        var cart = cartService.getOrCreateCart(customerId);
        var cartId = cart.id();

        // When/Then
        assertTrue(cartService.isCartActive(cartId));

        // After deactivation
        cartService.deactivateCart(cartId);
        assertFalse(cartService.isCartActive(cartId));
    }

    @Test
    @Transactional
    void testGetCartItemCount() {
        // Given - Use the pre-created customer
        // customerId is already set up in @BeforeEach

        // When (no cart exists)
        var initialCount = cartService.getCartItemCount(customerId);

        // Then
        assertEquals(0, initialCount);

        // When (empty cart exists)
        cartService.getOrCreateCart(customerId);
        var emptyCartCount = cartService.getCartItemCount(customerId);

        // Then
        assertEquals(0, emptyCartCount);
    }

    @Test
    @Transactional
    void testCalculateCartTotals() {
        // Given - Use the pre-created customer
        var cart = cartService.getOrCreateCart(customerId);

        // When
        var summary = cartService.calculateCartTotals(cart.id());

        // Then
        assertNotNull(summary);
        assertEquals(0, summary.totalItems());
        assertEquals(BigDecimal.ZERO, summary.totalAmount());
        assertTrue(summary.isEmpty());
        assertFalse(summary.isExpired()); // Cart should not be expired immediately
    }

    @Test
    @Transactional
    void testTransferGuestCartToCustomerWithNoGuestCart() {
        // Given
        var sessionId = "nonexistent-session";
        // Use the pre-created customer
        var testCustomerId = customerId;

        // When
        var result = cartService.transferGuestCartToCustomer(sessionId, testCustomerId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testTransferGuestCartToCustomerWithNoExistingCustomerCart() {
        // Given
        var sessionId = "test-session-" + UUID.randomUUID().toString();
        // Use the second pre-created customer to avoid conflicts
        var testCustomerId = customerId2;

        // Create guest cart
        var guestCart = cartService.getOrCreateGuestCart(sessionId);

        // When
        var result = cartService.transferGuestCartToCustomer(sessionId, testCustomerId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testCustomerId, result.get().customerId());
        assertEquals(guestCart.id(), result.get().id());
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
