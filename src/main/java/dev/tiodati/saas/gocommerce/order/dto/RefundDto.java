package dev.tiodati.saas.gocommerce.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing a refund.
 * 
 * @param id The refund ID
 * @param orderId The order ID that was refunded
 * @param orderNumber The order number that was refunded
 * @param refundNumber The unique refund number
 * @param refundType The type of refund
 * @param status The refund status
 * @param refundAmount The refund amount
 * @param processedAmount The amount actually processed (may differ from requested)
 * @param reason The refund reason
 * @param refundMethod The refund method
 * @param items List of refunded items
 * @param notes Additional notes
 * @param requestedDate When the refund was requested
 * @param processedDate When the refund was processed
 * @param createdAt Creation timestamp
 * @param updatedAt Last update timestamp
 * @param version Entity version for optimistic locking
 */
public record RefundDto(
        UUID id,
        UUID orderId,
        String orderNumber,
        String refundNumber,
        String refundType,
        String status,
        BigDecimal refundAmount,
        BigDecimal processedAmount,
        String reason,
        String refundMethod,
        List<RefundItemDto> items,
        String notes,
        Instant requestedDate,
        Instant processedDate,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {

    /**
     * DTO for refunded item details.
     * 
     * @param id The refund item ID
     * @param refundId The parent refund ID
     * @param orderItemId The original order item ID
     * @param productId The product ID
     * @param productName The product name
     * @param productSku The product SKU
     * @param quantity The refunded quantity
     * @param unitPrice The unit price of the item
     * @param refundAmount The total refund amount for this item
     * @param createdAt Creation timestamp
     * @param updatedAt Last update timestamp
     * @param version Entity version
     */
    public record RefundItemDto(
            UUID id,
            UUID refundId,
            UUID orderItemId,
            UUID productId,
            String productName,
            String productSku,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal refundAmount,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
