package dev.tiodati.saas.gocommerce.cart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for CartItem information.
 *
 * @param id          Cart item unique identifier
 * @param cartId      Shopping cart ID
 * @param productId   Product ID
 * @param productName Product name
 * @param productSku  Product SKU
 * @param quantity    Quantity in cart
 * @param unitPrice   Unit price
 * @param totalPrice  Total price for this line item
 * @param createdAt   Timestamp when the item was added to cart
 * @param updatedAt   Timestamp when the item was last updated
 */
public record CartItemDto(
        UUID id,
        UUID cartId,
        UUID productId,
        String productName,
        String productSku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
