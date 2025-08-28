package dev.tiodati.saas.gocommerce.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductKitDto(
    UUID id,
    UUID storeId,
    String name,
    String description,
    BigDecimal price,
    boolean isActive,
    List<KitItemDto> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
