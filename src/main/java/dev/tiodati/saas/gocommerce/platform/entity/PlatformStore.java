package dev.tiodati.saas.gocommerce.platform.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "PlatformStore")
@Table(name = "platform_stores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformStore {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "subdomain", nullable = false, unique = true)
    private String subdomain;
    
    @Column(name = "domain_suffix")
    private String domainSuffix;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StoreStatus status;
    
    @Column(name = "keycloak_realm_id")
    private String keycloakRealmId;
    
    @Column(name = "database_schema")
    private String databaseSchema;
    
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    public String getFullDomain() {
        return this.subdomain + "." + (this.domainSuffix != null ? this.domainSuffix : "gocommerce.com");
    }
}