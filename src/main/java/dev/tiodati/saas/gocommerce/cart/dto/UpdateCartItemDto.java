package dev.tiodati.saas.gocommerce.cart.dto;

import jakarta.validation.constraints.Min;

/**
 * DTO for updating a cart item quantity.
 * Used when modifying existing items in the cart.
 * @param quantity the new quantity for the cart item, must be at least 1
 */
public record UpdateCartItemDto(
        @Min(value = 1, message = "Quantity must be at least 1") int quantity) {
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
