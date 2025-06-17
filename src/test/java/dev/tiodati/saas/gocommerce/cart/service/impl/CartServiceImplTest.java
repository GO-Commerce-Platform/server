package dev.tiodati.saas.gocommerce.cart.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.cart.dto.CreateCartItemDto;
import dev.tiodati.saas.gocommerce.cart.dto.UpdateCartItemDto;
import dev.tiodati.saas.gocommerce.cart.service.CartService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Integration tests for CartService implementation.
 * Tests the complete cart workflow including cart and item management.
 */
@QuarkusTest
class CartServiceImplTest {

    /**
     * Injected CartService instance for testing.
     */
    @Inject
    private CartService cartService;

    @Test
    @Transactional
    void testGetOrCreateCartForNewCustomer() {
        // Given
        var customerId = UUID.randomUUID();

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
        // Given
        var customerId = UUID.randomUUID();
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
        // Given
        var customerId = UUID.randomUUID();
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
        // Given
        var customerId = UUID.randomUUID();
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
        // Given
        var customerId = UUID.randomUUID();
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
        // Given
        var customerId = UUID.randomUUID();
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
    @Transactional
    void testDeactivateCart() {
        // Given
        var customerId = UUID.randomUUID();
        var cart = cartService.getOrCreateCart(customerId);

        // When
        cartService.deactivateCart(cart.id());

        // Then
        assertFalse(cartService.isCartActive(cart.id()));

        // Deactivated cart should not be returned by getCartById
        var retrievedCart = cartService.getCartById(cart.id());
        assertFalse(retrievedCart.isPresent());
    }

    @Test
    @Transactional
    void testIsCartActive() {
        // Given
        var customerId = UUID.randomUUID();
        var cart = cartService.getOrCreateCart(customerId);

        // When/Then
        assertTrue(cartService.isCartActive(cart.id()));

        // After deactivation
        cartService.deactivateCart(cart.id());
        assertFalse(cartService.isCartActive(cart.id()));
    }

    @Test
    @Transactional
    void testGetCartItemCount() {
        // Given
        var customerId = UUID.randomUUID();

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
        // Given
        var customerId = UUID.randomUUID();
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
        var customerId = UUID.randomUUID();

        // When
        var result = cartService.transferGuestCartToCustomer(sessionId, customerId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testTransferGuestCartToCustomerWithNoExistingCustomerCart() {
        // Given
        var sessionId = "test-session-" + UUID.randomUUID().toString();
        var customerId = UUID.randomUUID();

        // Create guest cart
        var guestCart = cartService.getOrCreateGuestCart(sessionId);

        // When
        var result = cartService.transferGuestCartToCustomer(sessionId, customerId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(customerId, result.get().customerId());
        assertEquals(guestCart.id(), result.get().id());
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
