package dev.tiodati.saas.gocommerce.inventory.repository;

import dev.tiodati.saas.gocommerce.inventory.entity.InventoryAdjustment;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for InventoryAdjustment entity operations.
 * Provides database access methods for inventory adjustment tracking.
 */
@ApplicationScoped
public class InventoryAdjustmentRepository implements PanacheRepositoryBase<InventoryAdjustment, UUID> {

    /**
     * Find adjustments by product ID.
     *
     * @param productId the product ID
     * @param page the page information
     * @return list of adjustments for the product
     */
    public List<InventoryAdjustment> findByProductId(UUID productId, Page page) {
        return find("productId = ?1 ORDER BY adjustedAt DESC", productId)
                .page(page)
                .list();
    }

    /**
     * Find adjustments by product ID within a date range.
     *
     * @param productId the product ID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param page the page information
     * @return list of adjustments within the date range
     */
    public List<InventoryAdjustment> findByProductIdAndDateRange(UUID productId, Instant startDate, Instant endDate, Page page) {
        return find("productId = ?1 AND adjustedAt >= ?2 AND adjustedAt <= ?3 ORDER BY adjustedAt DESC", 
                   productId, startDate, endDate)
                .page(page)
                .list();
    }

    /**
     * Find recent adjustments for a product.
     *
     * @param productId the product ID
     * @param limit maximum number of records to return
     * @return list of recent adjustments
     */
    public List<InventoryAdjustment> findRecentByProductId(UUID productId, int limit) {
        return find("productId = ?1 ORDER BY adjustedAt DESC", productId)
                .page(Page.of(0, limit))
                .list();
    }

    /**
     * Find adjustments by type.
     *
     * @param adjustmentType the adjustment type
     * @param page the page information
     * @return list of adjustments of the specified type
     */
    public List<InventoryAdjustment> findByType(InventoryAdjustment.AdjustmentType adjustmentType, Page page) {
        return find("adjustmentType = ?1 ORDER BY adjustedAt DESC", adjustmentType)
                .page(page)
                .list();
    }

    /**
     * Find adjustments within a date range.
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param page the page information
     * @return list of adjustments within the date range
     */
    public List<InventoryAdjustment> findByDateRange(Instant startDate, Instant endDate, Page page) {
        return find("adjustedAt >= ?1 AND adjustedAt <= ?2 ORDER BY adjustedAt DESC", startDate, endDate)
                .page(page)
                .list();
    }

    /**
     * Count total adjustments for a product.
     *
     * @param productId the product ID
     * @return total number of adjustments for the product
     */
    public long countByProductId(UUID productId) {
        return count("productId = ?1", productId);
    }

    /**
     * Count adjustments by type for a product.
     *
     * @param productId the product ID
     * @param adjustmentType the adjustment type
     * @return number of adjustments of the specified type for the product
     */
    public long countByProductIdAndType(UUID productId, InventoryAdjustment.AdjustmentType adjustmentType) {
        return count("productId = ?1 AND adjustmentType = ?2", productId, adjustmentType);
    }

    /**
     * Find the most recent adjustment for a product.
     *
     * @param productId the product ID
     * @return the most recent adjustment, or null if none exists
     */
    public InventoryAdjustment findLatestByProductId(UUID productId) {
        return find("productId = ?1 ORDER BY adjustedAt DESC", productId)
                .firstResult();
    }

    /**
     * Calculate total quantity change for a product within a date range.
     *
     * @param productId the product ID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return total net quantity change (can be negative)
     */
    @SuppressWarnings("unchecked")
    public Integer getTotalQuantityChange(UUID productId, Instant startDate, Instant endDate) {
        String query = """
            SELECT 
                COALESCE(
                    SUM(
                        CASE 
                            WHEN adjustmentType = 'INCREASE' THEN quantity
                            WHEN adjustmentType = 'DECREASE' THEN -quantity
                            WHEN adjustmentType = 'SET' THEN (newQuantity - previousQuantity)
                            ELSE 0
                        END
                    ), 0
                )
            FROM InventoryAdjustment 
            WHERE productId = ?1 AND adjustedAt >= ?2 AND adjustedAt <= ?3
            """;
        
        var result = getEntityManager().createQuery(query, Long.class)
                .setParameter(1, productId)
                .setParameter(2, startDate)
                .setParameter(3, endDate)
                .getSingleResult();
        
        return result != null ? result.intValue() : 0;
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
