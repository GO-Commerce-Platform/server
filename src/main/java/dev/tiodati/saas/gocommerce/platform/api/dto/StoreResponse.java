package dev.tiodati.saas.gocommerce.platform.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StoreResponse(
    UUID id,
    String name,
    String subdomain,
    String fullDomain,
    String status,
    OffsetDateTime createdAt
) {}
