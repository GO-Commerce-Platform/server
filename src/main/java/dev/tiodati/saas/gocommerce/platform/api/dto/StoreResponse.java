package dev.tiodati.saas.gocommerce.platform.api.dto;

import java.time.Instant;
import java.util.UUID;

public record StoreResponse(
    UUID id,
    String storeName,
    String subdomain,
    String fullDomain,
    String status,
    Instant createdAt
) {}
