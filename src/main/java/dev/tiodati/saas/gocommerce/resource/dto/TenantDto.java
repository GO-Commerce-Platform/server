package dev.tiodati.saas.gocommerce.resource.dto;

import dev.tiodati.saas.gocommerce.model.tenant.TenantStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantDto {
    private Long id;
    private String tenantKey;
    private String name;
    private String subdomain;
    private TenantStatus status;
    private String billingPlan;
    private String settings;
}