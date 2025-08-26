package dev.tiodati.saas.gocommerce.promotion.dto;

import dev.tiodati.saas.gocommerce.promotion.entity.PromotionConfig.PromotionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a complete promotion.
 */
public record PromotionDto(
        UUID id,
        UUID storeId,
        String code,
        String name,
        String description,
        PromotionType type,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        Instant validFrom,
        Instant validUntil,
        Integer usageLimit,
        Integer usageCount,
        Boolean active,
        Integer priority,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {}
