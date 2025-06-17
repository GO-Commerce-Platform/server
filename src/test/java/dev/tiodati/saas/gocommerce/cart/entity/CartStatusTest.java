package dev.tiodati.saas.gocommerce.cart.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.customer.entity.Customer;
import dev.tiodati.saas.gocommerce.product.entity.Product;

/**
 * Unit tests for CartStatus enum and cart entities.
 */
class CartStatusTest {

    @Test
    void testCartStatusEnumValues() {
        // Verify all expected enum values exist
        assertEquals(4, CartStatus.values().length);
        assertEquals(CartStatus.ACTIVE, CartStatus.valueOf("ACTIVE"));
        assertEquals(CartStatus.ABANDONED, CartStatus.valueOf("ABANDONED"));
        assertEquals(CartStatus.CONVERTED, CartStatus.valueOf("CONVERTED"));
        assertEquals(CartStatus.EXPIRED, CartStatus.valueOf("EXPIRED"));
    }

    @Test
    void testShoppingCartWithCartStatus() {
        // Given
        var customer = Customer.builder()
                .id(UUID.randomUUID())
                .build();

        // When
        var cart = ShoppingCart.builder()
                .customer(customer)
                .status(CartStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        // Then
        assertNotNull(cart);
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
        assertEquals(customer, cart.getCustomer());
        assertNotNull(cart.getExpiresAt());
    }

    @Test
    void testCartItemCreation() {
        // Given
        var cart = ShoppingCart.builder()
                .status(CartStatus.ACTIVE)
                .build();

        var product = Product.builder()
                .id(UUID.randomUUID())
                .name("Test Product")
                .price(BigDecimal.valueOf(29.99))
                .build();

        // When
        var cartItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(2)
                .unitPrice(product.getPrice())
                .build();

        // Then
        assertNotNull(cartItem);
        assertEquals(cart, cartItem.getCart());
        assertEquals(product, cartItem.getProduct());
        assertEquals(2, cartItem.getQuantity());
        assertEquals(BigDecimal.valueOf(29.99), cartItem.getUnitPrice());
        assertEquals(BigDecimal.valueOf(59.98), cartItem.getTotalPrice());
    }

    @Test
    void testCartStatusDefaultValue() {
        // When
        var cart = ShoppingCart.builder().build();

        // Then
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
