package dev.tiodati.saas.gocommerce.product.dto;

import java.util.UUID;

public record KitItemDto(
    UUID id,
    UUID productId,
    int quantity
) {}
