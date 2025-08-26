package dev.tiodati.saas.gocommerce.promotion.dto;

import dev.tiodati.saas.gocommerce.promotion.entity.PromotionConfig.PromotionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for creating a new promotion.
 */
public record CreatePromotionDto(
        @NotBlank
        @Size(max = 100)
        String code,

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull
        PromotionType type,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 10, fraction = 4)
        BigDecimal discountValue,

        @DecimalMin(value = "0.0")
        @Digits(integer = 10, fraction = 2)
        BigDecimal minimumOrderAmount,

        @DecimalMin(value = "0.0")
        @Digits(integer = 10, fraction = 2)
        BigDecimal maximumDiscountAmount,

        @NotNull
        Instant validFrom,

        @NotNull
        Instant validUntil,

        @Min(1)
        Integer usageLimit,

        Boolean active,

        @Min(0)
        Integer priority
) {
        public CreatePromotionDto {
                // Set defaults
                if (active == null) {
                        active = true;
                }
                if (priority == null) {
                        priority = 0;
                }
        }
}
