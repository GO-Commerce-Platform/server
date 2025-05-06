package dev.tiodati.saas.gocommerce.store.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "store")
@Getter
@Setter
@NoArgsConstructor
public class Store extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;
    
    @Column(name = "store_key", nullable = false, unique = true)
    private String storeKey;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String subdomain;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus status = StoreStatus.TRIAL;
    
    @Column(name = "schema_name", nullable = false, unique = true)
    private String schemaName;
    
    @Column(name = "billing_plan", nullable = false)
    private String billingPlan = "BASIC";
    
    @Column(columnDefinition = "json")
    private String settings;
    
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
    
    @Version
    private int version;
    
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