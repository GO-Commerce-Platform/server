package dev.tiodati.saas.gocommerce.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO for creating a new cart item.
 * Contains the minimal information needed to add a product to a cart.
 * @param productId the ID of the product to be added to the cart
 * @param quantity the quantity of the product to be added, must be at least 1
 */
public record CreateCartItemDto(
        @NotNull(message = "Product ID is required") UUID productId,

        @Min(value = 1, message = "Quantity must be at least 1") int quantity) {
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
