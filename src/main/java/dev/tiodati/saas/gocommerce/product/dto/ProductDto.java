package dev.tiodati.saas.gocommerce.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for product information
 */
public record ProductDto(
    UUID id,
    String sku,
    String name,
    String description,
    BigDecimal price,
    BigDecimal cost,
    int stockQuantity,
    boolean isActive,
    UUID categoryId,
    Instant createdAt,
    Instant updatedAt
) {}
