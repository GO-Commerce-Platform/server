package dev.tiodati.saas.gocommerce.product.dto;

import java.util.UUID;

public record CreateKitItemDto(
    UUID productId,
    int quantity
) {}
