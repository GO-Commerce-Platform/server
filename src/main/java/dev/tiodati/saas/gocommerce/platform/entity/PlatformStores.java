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

    /**
     * Unique identifier for the store. This is a UUID that is generated when
     * the store is created. It serves as the primary key for the store entity.
     **/
    @Id
    @Column(name = "id")
    private UUID id;

    /**
     * Subdomain for the store. This is a unique identifier that is used to
     * access the store. It is required and must be unique across all stores.
     **/
    @Column(name = "subdomain", unique = true, nullable = false)
    private String subdomain;

    /**
     * Domain suffix for the store. This is the part of the domain that comes
     * after the subdomain. It can be null or empty, in which case the full
     * domain will just be the subdomain.
     **/
    @Column(name = "domain_suffix")
    private String domainSuffix;

    /**
     * Name of the store. This is a human-readable name for the store. It is
     * required and cannot be null.
     **/
    @Column(name = "schema_name", unique = true, nullable = false)
    private String schemaName;

    /**
     * Name of the store as displayed to users. This is the name that will be
     * shown in the user interface. It is required and cannot be null.
     **/
    @Column(name = "store_name", nullable = false)
    private String storeName;

    /**
     * Description of the store. This is a brief description of the store's
     * purpose or offerings. It can be null or empty.
     **/
    @Column(name = "description")
    private String description;

    /**
     * Email address associated with the store. This is used for communication
     * and notifications related to the store. It is required and cannot be
     * null.
     **/
    @Column(name = "email", nullable = false)
    private String email;

    /**
     * Currency code for the store. This is the ISO 4217 currency code that
     * represents the currency used in the store. It is required and cannot be
     * null.
     **/
    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    /**
     * Default locale for the store. This is the language and region code that
     * represents the default locale used in the store. It is required and
     * cannot be null.
     **/
    @Column(name = "default_locale", nullable = false)
    private String defaultLocale;

    /**
     * Locale for the store. This is the language and region code that
     * represents the locale used in the store. It is required and cannot be
     * null.
     **/
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StoreStatus status;

    /**
     * Description of the store. This is a brief description of the store's
     * purpose or offerings. It can be null or empty.
     **/
    @Column(name = "owner_id")
    private String ownerId;

    /**
     * Keycloak realm ID associated with the store. This is used for
     * authentication and authorization purposes. It can be null or empty if
     * Keycloak integration is not used.
     **/
    @Column(name = "keycloak_realm_id")
    private String keycloakRealmId;

    /**
     * Configuration settings for the store. This is a JSON string that contains
     * various configuration options for the store. It can be null or empty if
     * no specific configuration is set.
     **/
    @Column(name = "configuration", columnDefinition = "json")
    private String configuration;

    /**
     * Timestamp when the store was created. This is automatically set when the
     * store is created and cannot be changed.
     **/
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Timestamp when the store was last updated. This is automatically updated
     * whenever the store entity is modified.
     **/
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Version of the store entity. This is used for optimistic locking to
     * prevent concurrent updates from overwriting each other. It is
     * automatically managed by Hibernate.
     **/
    @Version
    @Column(name = "version")
    private Long version;

    // Constructors
    public PlatformStores() {
        // Default constructor for JPA
    }

    /**
     * Returns the full domain for this store by combining subdomain and domain
     * suffix. If domain suffix is null or empty, returns just the subdomain.
     *
     * @return The full domain for the store
     */
    public String getFullDomain() {
        if (domainSuffix == null || domainSuffix.isEmpty()) {
            return subdomain;
        }
        return subdomain + "." + domainSuffix;
    }

    /**
     * Checks if the store is deleted. A store is considered deleted if its
     * status is set to DELETED.
     * @return true if the store is deleted, false otherwise
     */
    public boolean isDeleted() {
        return StoreStatus.DELETED.equals(status);
    }
}
