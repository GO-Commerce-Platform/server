package dev.tiodati.saas.gocommerce.model.tenant;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_key", nullable = false, unique = true)
    private String tenantKey;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String subdomain;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.TRIAL;
    
    @Column(name = "schema_name", nullable = false, unique = true)
    private String schemaName;
    
    @Column(name = "billing_plan", nullable = false)
    private String billingPlan = "BASIC";
    
    @Column(columnDefinition = "json")
    private String settings;
    
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }
    
    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}