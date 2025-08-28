package dev.tiodati.saas.gocommerce.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductKitDto(
    String name,
    String description,
    BigDecimal price,
    boolean isActive,
    List<UpdateKitItemDto> items
) {}
