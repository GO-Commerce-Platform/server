package dev.tiodati.saas.gocommerce.product.dto;

import java.util.UUID;

public record UpdateKitItemDto(
    UUID id,
    UUID productId,
    int quantity
) {}
