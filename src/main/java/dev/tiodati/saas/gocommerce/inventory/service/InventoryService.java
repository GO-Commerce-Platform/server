package dev.tiodati.saas.gocommerce.inventory.service;

import dev.tiodati.saas.gocommerce.inventory.dto.InventoryAdjustmentDto;
import dev.tiodati.saas.gocommerce.inventory.dto.InventoryReportDto;
import dev.tiodati.saas.gocommerce.inventory.dto.InventoryUpdateDto;
import dev.tiodati.saas.gocommerce.inventory.dto.LowStockAlertDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for inventory management operations.
 * Provides comprehensive inventory tracking, adjustments, and reporting.
 */
public interface InventoryService {

    /**
     * Update stock levels for a specific product.
     * This operation is atomic and includes validation.
     *
     * @param storeId The store ID
     * @param productId The product ID
     * @param updateDto Stock update information
     * @return true if update was successful
     */
    boolean updateProductInventory(UUID storeId, UUID productId, InventoryUpdateDto updateDto);

    /**
     * Record an inventory adjustment with full audit trail.
     * This creates both the stock change and audit record.
     *
     * @param storeId The store ID
     * @param adjustmentDto Adjustment information
     * @return true if adjustment was recorded successfully
     */
    boolean recordInventoryAdjustment(UUID storeId, InventoryAdjustmentDto adjustmentDto);

    /**
     * Get products with low stock levels.
     * Returns products where current stock <= low stock threshold.
     *
     * @param storeId The store ID
     * @param limit Maximum number of results (optional)
     * @param urgencyLevel Filter by urgency level (optional)
     * @return List of low stock alerts
     */
    List<LowStockAlertDto> getLowStockAlerts(UUID storeId, Integer limit, LowStockAlertDto.UrgencyLevel urgencyLevel);

    /**
     * Generate comprehensive inventory reports.
     *
     * @param storeId The store ID
     * @param reportType Type of report to generate
     * @param includeCategoryBreakdown Whether to include category breakdown
     * @return Inventory report with statistics
     */
    InventoryReportDto generateInventoryReport(UUID storeId, InventoryReportDto.ReportType reportType, boolean includeCategoryBreakdown);

    /**
     * Check if a product has sufficient stock for a given quantity.
     *
     * @param storeId The store ID
     * @param productId The product ID
     * @param requiredQuantity Required quantity
     * @return true if sufficient stock is available
     */
    boolean hasSufficientStock(UUID storeId, UUID productId, Integer requiredQuantity);

    /**
     * Reserve stock for a product (for order processing).
     * This temporarily reduces available stock without permanent adjustment.
     *
     * @param storeId The store ID
     * @param productId The product ID
     * @param quantity Quantity to reserve
     * @param reservationId Unique reservation identifier
     * @return true if reservation was successful
     */
    boolean reserveStock(UUID storeId, UUID productId, Integer quantity, String reservationId);

    /**
     * Release previously reserved stock.
     *
     * @param storeId The store ID
     * @param reservationId Unique reservation identifier
     * @return true if release was successful
     */
    boolean releaseStockReservation(UUID storeId, String reservationId);

    /**
     * Confirm reserved stock (convert reservation to actual stock reduction).
     *
     * @param storeId The store ID
     * @param reservationId Unique reservation identifier
     * @param reason Reason for the stock reduction
     * @return true if confirmation was successful
     */
    boolean confirmStockReservation(UUID storeId, String reservationId, String reason);

    /**
     * Get stock level for a specific product.
     *
     * @param storeId The store ID
     * @param productId The product ID
     * @return Current stock level, or null if product not found
     */
    Integer getCurrentStockLevel(UUID storeId, UUID productId);

    /**
     * Bulk update stock levels for multiple products.
     * This is an atomic operation - either all updates succeed or all fail.
     *
     * @param storeId The store ID
     * @param updates List of stock updates
     * @return true if all updates were successful
     */
    boolean bulkUpdateInventory(UUID storeId, List<InventoryUpdateDto> updates);
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
