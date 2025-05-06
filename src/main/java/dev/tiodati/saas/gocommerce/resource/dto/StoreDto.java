package dev.tiodati.saas.gocommerce.resource.dto;

import java.util.UUID;

import dev.tiodati.saas.gocommerce.store.model.StoreStatus;

public record StoreDto(
    UUID id,
    String storeKey,
    String name,
    String subdomain,
    StoreStatus status,
    String billingPlan,
    String settings
) {}