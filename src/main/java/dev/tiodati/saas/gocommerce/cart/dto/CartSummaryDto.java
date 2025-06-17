package dev.tiodati.saas.gocommerce.cart.dto;

import java.math.BigDecimal;

/**
 * DTO for cart summary information.
 * Provides a lightweight representation of cart totals and item count.
 * @param totalItems the total number of items in the cart
 * @param totalAmount the total monetary amount of the cart
 * @param isEmpty indicates if the cart is empty
 * @param isExpired indicates if the cart has expired
 */
public record CartSummaryDto(
        int totalItems,
        BigDecimal totalAmount,
        boolean isEmpty,
        boolean isExpired) {
    /**
     * Creates an empty cart summary.
     */
    public static CartSummaryDto empty() {
        return new CartSummaryDto(0, BigDecimal.ZERO, true, false);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
