package dev.tiodati.saas.gocommerce.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Data Transfer Object for recording inventory adjustments with audit trail.
 *
 * @param productId       Product unique identifier
 * @param adjustmentType  Type of adjustment (INCREASE, DECREASE, SET)
 * @param quantity        Quantity to adjust (positive for increase, negative for decrease, absolute for set)
 * @param reason          Reason for the adjustment
 * @param reference       Optional reference number (PO, transfer, etc.)
 * @param notes           Additional notes for the adjustment
 */
public record InventoryAdjustmentDto(
        @NotNull UUID productId,
        @NotNull AdjustmentType adjustmentType,
        @NotNull Integer quantity,
        @NotBlank String reason,
        String reference,
        String notes) {

    /**
     * Type of inventory adjustment.
     */
    public enum AdjustmentType {
        INCREASE,  // Add to current stock
        DECREASE,  // Remove from current stock
        SET        // Set to absolute value
    }

    /**
     * Creates an increase adjustment.
     */
    public static InventoryAdjustmentDto increase(UUID productId, Integer quantity, String reason) {
        return new InventoryAdjustmentDto(productId, AdjustmentType.INCREASE, quantity, reason, null, null);
    }

    /**
     * Creates a decrease adjustment.
     */
    public static InventoryAdjustmentDto decrease(UUID productId, Integer quantity, String reason) {
        return new InventoryAdjustmentDto(productId, AdjustmentType.DECREASE, quantity, reason, null, null);
    }

    /**
     * Creates a set adjustment.
     */
    public static InventoryAdjustmentDto set(UUID productId, Integer quantity, String reason) {
        return new InventoryAdjustmentDto(productId, AdjustmentType.SET, quantity, reason, null, null);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
