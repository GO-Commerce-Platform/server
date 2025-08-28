package dev.tiodati.saas.gocommerce.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductKitDto(
    String name,
    String description,
    BigDecimal price,
    boolean isActive,
    List<CreateKitItemDto> items
) {}
