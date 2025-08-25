package dev.tiodati.saas.gocommerce.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object for inventory reports and statistics.
 *
 * @param reportType       Type of report (SUMMARY, DETAILED, LOW_STOCK)
 * @param generatedAt      When the report was generated
 * @param totalProducts    Total number of products being tracked
 * @param totalStockValue  Total value of all stock
 * @param lowStockCount    Number of products with low stock
 * @param outOfStockCount  Number of products out of stock
 * @param totalStockUnits  Total inventory units across all products
 * @param averageStockLevel Average stock level across products
 * @param topLowStockItems Top items that need restocking
 * @param categoryBreakdown Stock breakdown by category
 * @param periodComparison  Period-over-period comparison data
 */
public record InventoryReportDto(
        ReportType reportType,
        Instant generatedAt,
        Integer totalProducts,
        BigDecimal totalStockValue,
        Integer lowStockCount,
        Integer outOfStockCount,
        Integer totalStockUnits,
        Double averageStockLevel,
        List<LowStockAlertDto> topLowStockItems,
        List<CategoryStockDto> categoryBreakdown,
        PeriodComparisonDto periodComparison) {

    /**
     * Type of inventory report.
     */
    public enum ReportType {
        SUMMARY,     // High-level overview
        DETAILED,    // Complete inventory details
        LOW_STOCK,   // Focus on low stock items
        VALUATION    // Focus on stock valuation
    }

    /**
     * Stock information by category.
     */
    public record CategoryStockDto(
            String categoryName,
            Integer productCount,
            Integer totalStockUnits,
            BigDecimal totalStockValue,
            Integer lowStockProducts) {
    }

    /**
     * Period-over-period comparison data.
     */
    public record PeriodComparisonDto(
            String comparisonPeriod,
            Integer previousTotalProducts,
            Integer previousLowStockCount,
            Integer previousOutOfStockCount,
            BigDecimal previousTotalValue,
            Double productCountChange,
            Double lowStockChange,
            Double outOfStockChange,
            Double totalValueChange) {
    }

    /**
     * Creates a summary report.
     */
    public static InventoryReportDto createSummary(
            Integer totalProducts,
            BigDecimal totalStockValue,
            Integer lowStockCount,
            Integer outOfStockCount,
            Integer totalStockUnits,
            Double averageStockLevel) {
        return new InventoryReportDto(
                ReportType.SUMMARY,
                Instant.now(),
                totalProducts,
                totalStockValue,
                lowStockCount,
                outOfStockCount,
                totalStockUnits,
                averageStockLevel,
                null,
                null,
                null
        );
    }

    /**
     * Gets stock health percentage (0-100).
     */
    public int getStockHealthPercentage() {
        if (totalProducts == null || totalProducts == 0) {
            return 100;
        }
        
        int unhealthyProducts = (lowStockCount != null ? lowStockCount : 0) + 
                                (outOfStockCount != null ? outOfStockCount : 0);
        int healthyProducts = totalProducts - unhealthyProducts;
        
        return Math.max(0, (healthyProducts * 100) / totalProducts);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
