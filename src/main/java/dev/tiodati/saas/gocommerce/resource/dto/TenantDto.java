package dev.tiodati.saas.gocommerce.resource.dto;

import dev.tiodati.saas.gocommerce.model.tenant.TenantStatus;

import java.util.UUID;

public record TenantDto(
    UUID id,
    String tenantKey,
    String name,
    String subdomain,
    TenantStatus status,
    String billingPlan,
    String settings
) {}