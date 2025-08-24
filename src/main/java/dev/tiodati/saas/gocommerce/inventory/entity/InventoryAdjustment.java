package dev.tiodati.saas.gocommerce.inventory.entity;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity for tracking inventory adjustments with complete audit trail.
 * This provides accountability and traceability for all inventory changes.
 */
@Entity
@Table(name = "inventory_adjustment")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustment extends PanacheEntityBase {

    /**
     * Unique identifier for the adjustment.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * Product that was adjusted.
     */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /**
     * Type of adjustment performed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false)
    private AdjustmentType adjustmentType;

    /**
     * Quantity adjusted (positive/negative based on type).
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Stock quantity before the adjustment.
     */
    @Column(name = "previous_quantity", nullable = false)
    private Integer previousQuantity;

    /**
     * Stock quantity after the adjustment.
     */
    @Column(name = "new_quantity", nullable = false)
    private Integer newQuantity;

    /**
     * Reason for the adjustment.
     */
    @Column(name = "reason", nullable = false)
    private String reason;

    /**
     * Optional reference number (PO, transfer, etc.).
     */
    @Column(name = "reference")
    private String reference;

    /**
     * Additional notes about the adjustment.
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * User who performed the adjustment.
     */
    @Column(name = "adjusted_by")
    private String adjustedBy;

    /**
     * When the adjustment was recorded.
     */
    @Column(name = "adjusted_at", nullable = false, updatable = false)
    private Instant adjustedAt;

    /**
     * Type of inventory adjustment.
     */
    public enum AdjustmentType {
        INCREASE,  // Add to current stock
        DECREASE,  // Remove from current stock
        SET        // Set to absolute value
    }

    @PrePersist
    void prePersist() {
        if (adjustedAt == null) {
            adjustedAt = Instant.now();
        }
        
        // Calculate actual quantity change based on type
        if (adjustmentType != null && previousQuantity != null && newQuantity != null) {
            switch (adjustmentType) {
                case INCREASE -> quantity = Math.abs(newQuantity - previousQuantity);
                case DECREASE -> quantity = Math.abs(previousQuantity - newQuantity);
                case SET -> quantity = newQuantity;
            }
        }
    }

    /**
     * Gets the net change in quantity (considering direction).
     */
    public Integer getNetQuantityChange() {
        if (adjustmentType == null || quantity == null) {
            return 0;
        }
        
        return switch (adjustmentType) {
            case INCREASE -> quantity;
            case DECREASE -> -quantity;
            case SET -> newQuantity != null && previousQuantity != null 
                        ? newQuantity - previousQuantity 
                        : 0;
        };
    }

    /**
     * Checks if this adjustment resulted in a stock increase.
     */
    public boolean isIncrease() {
        return getNetQuantityChange() > 0;
    }

    /**
     * Checks if this adjustment resulted in a stock decrease.
     */
    public boolean isDecrease() {
        return getNetQuantityChange() < 0;
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
