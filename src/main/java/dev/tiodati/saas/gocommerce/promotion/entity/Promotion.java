package dev.tiodati.saas.gocommerce.promotion.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a promotion/discount in the system.
 * Supports various types of discounts with tenant/store isolation.
 */
@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false, length = 20)
    private PromotionType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "minimum_order_amount", precision = 12, scale = 2)
    private BigDecimal minimumOrderAmount;

    @DecimalMin(value = "0.0")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "maximum_discount_amount", precision = 12, scale = 2)
    private BigDecimal maximumDiscountAmount;

    @Size(max = 50)
    @Column(name = "promo_code", length = 50, unique = true)
    private String promoCode;

    @NotNull
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @NotNull
    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Builder.Default
    @Column(name = "usage_limit")
    private Integer usageLimit = null; // null means unlimited

    @Builder.Default
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @NotBlank
    @Size(max = 50)
    @Column(name = "store_id", nullable = false, length = 50)
    private String storeId; // For tenant isolation

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if the promotion is currently valid based on date range and usage limits.
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active &&
                now.isAfter(validFrom) &&
                now.isBefore(validUntil) &&
                (usageLimit == null || usedCount < usageLimit);
    }

    /**
     * Check if the promotion can be applied to an order of the given amount.
     */
    public boolean canApplyToOrder(BigDecimal orderAmount) {
        return isValid() && 
               (minimumOrderAmount == null || orderAmount.compareTo(minimumOrderAmount) >= 0);
    }

    /**
     * Calculate the discount amount for a given order total.
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!canApplyToOrder(orderAmount)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        switch (type) {
            case PERCENTAGE:
                discount = orderAmount.multiply(discountValue.divide(BigDecimal.valueOf(100)));
                break;
            case FIXED_AMOUNT:
                discount = discountValue;
                break;
            default:
                discount = BigDecimal.ZERO;
        }

        // Apply maximum discount limit if set
        if (maximumDiscountAmount != null && discount.compareTo(maximumDiscountAmount) > 0) {
            discount = maximumDiscountAmount;
        }

        // Ensure discount doesn't exceed order amount
        return discount.min(orderAmount);
    }
}
