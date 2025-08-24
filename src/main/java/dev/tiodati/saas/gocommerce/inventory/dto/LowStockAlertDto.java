package dev.tiodati.saas.gocommerce.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for low stock alerts.
 *
 * @param productId          Product unique identifier
 * @param productName        Product name
 * @param sku                Product SKU
 * @param currentStock       Current stock quantity
 * @param lowStockThreshold  Low stock threshold
 * @param category           Product category name
 * @param price              Current product price
 * @param lastUpdated        Last stock update timestamp
 * @param daysAtCurrentLevel Days the product has been at this stock level
 */
public record LowStockAlertDto(
        UUID productId,
        String productName,
        String sku,
        Integer currentStock,
        Integer lowStockThreshold,
        String category,
        BigDecimal price,
        Instant lastUpdated,
        Integer daysAtCurrentLevel) {

    /**
     * Gets the stock percentage relative to threshold.
     * 
     * @return percentage (0-100) where 100 = at threshold, 0 = out of stock
     */
    public int getStockPercentage() {
        if (lowStockThreshold <= 0) {
            return currentStock > 0 ? 100 : 0;
        }
        return Math.max(0, Math.min(100, (currentStock * 100) / lowStockThreshold));
    }

    /**
     * Gets urgency level based on current stock.
     * 
     * @return urgency level: CRITICAL (0 stock), HIGH (<25%), MEDIUM (<50%), LOW (>=50%)
     */
    public UrgencyLevel getUrgencyLevel() {
        if (currentStock <= 0) {
            return UrgencyLevel.CRITICAL;
        }
        
        int percentage = getStockPercentage();
        if (percentage < 25) {
            return UrgencyLevel.HIGH;
        } else if (percentage < 50) {
            return UrgencyLevel.MEDIUM;
        } else {
            return UrgencyLevel.LOW;
        }
    }

    /**
     * Urgency levels for low stock alerts.
     */
    public enum UrgencyLevel {
        CRITICAL, HIGH, MEDIUM, LOW
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
