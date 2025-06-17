package dev.tiodati.saas.gocommerce.cart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for representing a shopping cart with its items.
 * Contains complete cart information including items, totals, and metadata.
 * This is used for detailed cart views and operations.
 * @param id unique identifier for the shopping cart
 * @param customerId the ID of the customer who owns the cart
 * @param sessionId the session ID for guest carts
 * @param isActive indicates if the cart is currently active
 * @param expiresAt the expiration date and time of the cart
 * @param createdAt the date and time when the cart was created
 * @param updatedAt the date and time when the cart was last updated
 * @param items list of items in the cart, each represented by CartItemDto
 * @param totalItems total number of items in the cart
 * @param totalAmount total monetary amount of the cart
 */
public record ShoppingCartDto(
        UUID id,
        UUID customerId,
        String sessionId,
        boolean isActive,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CartItemDto> items,
        int totalItems,
        BigDecimal totalAmount) {
    /**
     * Creates a summary cart DTO without items.
     * Useful for cart listings and quick overviews.
     */
    public ShoppingCartDto withoutItems() {
        return new ShoppingCartDto(
                id,
                customerId,
                sessionId,
                isActive,
                expiresAt,
                createdAt,
                updatedAt,
                List.of(),
                totalItems,
                totalAmount);
    }

    /**
     * Checks if the cart is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if the cart is empty.
     */
    public boolean isEmpty() {
        return totalItems == 0 || items.isEmpty();
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
