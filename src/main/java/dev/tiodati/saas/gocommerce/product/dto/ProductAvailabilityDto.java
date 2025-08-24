package dev.tiodati.saas.gocommerce.product.dto;

import java.util.UUID;

/**
 * Data Transfer Object for Product Availability information.
 *
 * @param productId        Product unique identifier
 * @param available        Whether the product is available for purchase
 * @param inStock          Whether the product is currently in stock
 * @param inventoryQuantity Current inventory quantity (null if not tracked)
 * @param lowStock         Whether the product is running low on stock
 * @param reason           Reason why product is not available (if applicable)
 */
public record ProductAvailabilityDto(
        UUID productId,
        boolean available,
        boolean inStock,
        Integer inventoryQuantity,
        boolean lowStock,
        String reason) {
        
    /**
     * Creates availability DTO for an available product.
     */
    public static ProductAvailabilityDto available(UUID productId, Integer inventoryQuantity, boolean lowStock) {
        return new ProductAvailabilityDto(productId, true, true, inventoryQuantity, lowStock, null);
    }
    
    /**
     * Creates availability DTO for an out of stock product.
     */
    public static ProductAvailabilityDto outOfStock(UUID productId) {
        return new ProductAvailabilityDto(productId, false, false, 0, false, "Out of stock");
    }
    
    /**
     * Creates availability DTO for an inactive product.
     */
    public static ProductAvailabilityDto inactive(UUID productId, String reason) {
        return new ProductAvailabilityDto(productId, false, false, null, false, reason);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
