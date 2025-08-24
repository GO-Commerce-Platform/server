package dev.tiodati.saas.gocommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Data Transfer Object for updating inventory stock levels.
 *
 * @param productId        Product unique identifier
 * @param newQuantity      New stock quantity (must be non-negative)
 * @param lowStockThreshold Optional new low stock threshold
 * @param reason           Reason for the update (for audit purposes)
 */
public record InventoryUpdateDto(
        @NotNull UUID productId,
        @NotNull @Min(0) Integer newQuantity,
        @Min(1) Integer lowStockThreshold,
        String reason) {

    /**
     * Creates inventory update DTO with just quantity change.
     */
    public static InventoryUpdateDto quantityUpdate(UUID productId, Integer newQuantity) {
        return new InventoryUpdateDto(productId, newQuantity, null, "Quantity update");
    }

    /**
     * Creates inventory update DTO with quantity and threshold changes.
     */
    public static InventoryUpdateDto fullUpdate(UUID productId, Integer newQuantity, Integer lowStockThreshold, String reason) {
        return new InventoryUpdateDto(productId, newQuantity, lowStockThreshold, reason);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
