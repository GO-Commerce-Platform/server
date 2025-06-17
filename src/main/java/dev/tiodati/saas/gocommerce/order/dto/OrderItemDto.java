package dev.tiodati.saas.gocommerce.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for OrderItem information.
 *
 * @param id          Order item unique identifier
 * @param orderId     Order header ID
 * @param productId   Product ID
 * @param quantity    Quantity ordered
 * @param unitPrice   Unit price at time of order
 * @param totalPrice  Total price for this line item
 * @param productName Product name at time of order
 * @param productSku  Product SKU at time of order
 * @param createdAt   Timestamp when the order item was created
 * @param updatedAt   Timestamp when the order item was last updated
 * @param version     Version for optimistic locking
 */
public record OrderItemDto(
        UUID id,
        UUID orderId,
        UUID productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String productName,
        String productSku,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version) {
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
