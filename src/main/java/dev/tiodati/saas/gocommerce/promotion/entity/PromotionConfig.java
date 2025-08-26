package dev.tiodati.saas.gocommerce.promotion.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing promotion configurations for stores.
 * Supports tenant-scoped discount and promotion rules.
 */
@Entity
@Table(name = "promotion_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The store/tenant this promotion belongs to.
     */
    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    /**
     * Unique promotion code (e.g., "VIP10", "WELCOME5").
     */
    @Column(name = "code", nullable = false)
    private String code;

    /**
     * Human-readable promotion name.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Type of promotion.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PromotionType type;

    /**
     * Discount value - percentage (0.10 for 10%) or fixed amount.
     */
    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountValue;

    /**
     * Minimum order amount to qualify for this promotion.
     */
    @Column(name = "minimum_order_amount", precision = 19, scale = 2)
    private BigDecimal minimumOrderAmount;

    /**
     * Maximum discount amount (for percentage discounts).
     */
    @Column(name = "maximum_discount_amount", precision = 19, scale = 2)
    private BigDecimal maximumDiscountAmount;

    /**
     * When this promotion becomes active.
     */
    @Column(name = "valid_from")
    private Instant validFrom;

    /**
     * When this promotion expires.
     */
    @Column(name = "valid_until")
    private Instant validUntil;

    /**
     * Maximum number of times this promotion can be used.
     */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /**
     * Current number of times this promotion has been used.
     */
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    /**
     * Whether this promotion is currently active.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Priority for promotion application (higher number = higher priority).
     */
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    /**
     * Description of the promotion.
     */
    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Enum for promotion types.
     */
    public enum PromotionType {
        /**
         * Percentage discount (e.g., 10% off).
         */
        PERCENTAGE,
        
        /**
         * Fixed amount discount (e.g., $20 off).
         */
        FIXED_AMOUNT,
        
        /**
         * Volume-based discount (automatic based on order total).
         */
        VOLUME_BASED
    }

    /**
     * Checks if this promotion is currently valid and can be applied.
     *
     * @return true if the promotion is active and within valid date range
     */
    public boolean isCurrentlyValid() {
        if (!active) {
            return false;
        }

        var now = Instant.now();
        
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        
        if (validUntil != null && now.isAfter(validUntil)) {
            return false;
        }
        
        if (usageLimit != null && usageCount >= usageLimit) {
            return false;
        }
        
        return true;
    }

    /**
     * Checks if this promotion can be applied to the given order amount.
     *
     * @param orderAmount The order amount to check
     * @return true if the promotion qualifies for this order amount
     */
    public boolean qualifiesForOrderAmount(BigDecimal orderAmount) {
        if (minimumOrderAmount == null) {
            return true;
        }
        return orderAmount.compareTo(minimumOrderAmount) >= 0;
    }

    /**
     * Calculates the discount amount for the given order total.
     *
     * @param orderAmount The order amount
     * @return The calculated discount amount
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isCurrentlyValid() || !qualifiesForOrderAmount(orderAmount)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        
        switch (type) {
            case PERCENTAGE:
                discount = orderAmount.multiply(discountValue);
                // Apply maximum discount cap if specified
                if (maximumDiscountAmount != null && discount.compareTo(maximumDiscountAmount) > 0) {
                    discount = maximumDiscountAmount;
                }
                break;
                
            case FIXED_AMOUNT:
                discount = discountValue;
                // Don't apply more discount than the order total
                if (discount.compareTo(orderAmount) > 0) {
                    discount = orderAmount;
                }
                break;
                
            case VOLUME_BASED:
                discount = orderAmount.multiply(discountValue);
                break;
                
            default:
                discount = BigDecimal.ZERO;
        }

        return discount.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
