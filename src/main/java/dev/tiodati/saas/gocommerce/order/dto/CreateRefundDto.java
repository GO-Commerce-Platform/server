package dev.tiodati.saas.gocommerce.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating a refund request.
 * 
 * @param orderId The order to refund
 * @param refundType The type of refund (FULL, PARTIAL)
 * @param refundAmount The amount to refund (null for full refund)
 * @param items List of items to refund (null for full refund)
 * @param reason The reason for the refund
 * @param refundMethod The refund method (ORIGINAL_PAYMENT, STORE_CREDIT, CHECK, etc.)
 * @param notes Additional notes
 */
public record CreateRefundDto(
        UUID orderId,
        RefundType refundType,
        BigDecimal refundAmount,
        List<RefundItemDto> items,
        String reason,
        String refundMethod,
        String notes
) {

    /**
     * DTO for refund item details.
     * 
     * @param orderItemId The order item to refund
     * @param quantity The quantity to refund
     * @param refundAmount The amount to refund for this item (optional, calculated if null)
     */
    public record RefundItemDto(
            UUID orderItemId,
            int quantity,
            BigDecimal refundAmount
    ) {
    }

    /**
     * Enum for refund types.
     */
    public enum RefundType {
        FULL,    // Full order refund
        PARTIAL  // Partial refund (specific items or amount)
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
