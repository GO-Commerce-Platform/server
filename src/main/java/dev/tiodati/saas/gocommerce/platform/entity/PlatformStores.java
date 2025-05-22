package dev.tiodati.saas.gocommerce.platform.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "platform_stores")
@Builder
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PlatformStores extends PanacheEntityBase {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "subdomain", unique = true, nullable = false)
    private String subdomain;
    
    @Column(name = "domain_suffix")
    private String domainSuffix;
    
    @Column(name = "schema_name", unique = true, nullable = false)
    private String schemaName;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StoreStatus status;

    @Column(name = "owner_id")
    private String ownerId;
    
    @Column(name = "keycloak_realm_id")
    private String keycloakRealmId;

    @Column(name = "configuration", columnDefinition = "json")
    private String configuration;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;

    // Constructors
    public PlatformStores() {
        this.id = UUID.randomUUID();
        this.status = StoreStatus.CREATING;
    }
    
    /**
     * Returns the full domain for this store by combining subdomain and domain suffix.
     * If domain suffix is null or empty, returns just the subdomain.
     *
     * @return The full domain for the store
     */
    public String getFullDomain() {
        if (domainSuffix == null || domainSuffix.isEmpty()) {
            return subdomain;
        }
        return subdomain + "." + domainSuffix;
    }
}