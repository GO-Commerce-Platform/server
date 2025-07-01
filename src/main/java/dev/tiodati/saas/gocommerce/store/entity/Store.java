package dev.tiodati.saas.gocommerce.store.entity;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "store")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store extends PanacheEntityBase {

    /**
     * Unique identifier for the store. This is a UUID that is generated when
     * the store is created. It serves as the primary key for the store entity.
     **/
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * Unique key for the store. This is a string that is used to identify the
     * store in various contexts, such as in URLs or API requests.
     **/
    @Column(name = "store_key", nullable = false, unique = true)
    private String storeKey;

    /**
     * Name of the store. This is a human-readable name for the store. It is
     * required and cannot be null.
     **/
    @Column(nullable = false)
    private String name;

    /**
     * Subdomain for the store. This is a unique identifier that is used to
     * access the store. It is required and must be unique across all stores.
     **/
    @Column(nullable = false, unique = true)
    private String subdomain;

    /**
     * Domain suffix for the store. This is the part of the domain that comes
     * after the subdomain. It can be null or empty, in which case the full
     * domain will just be the subdomain.
     **/
    @Column(name = "domain_suffix")
    private String domainSuffix;

    /**
     * Description of the store. This is a brief description of the store\'s
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
    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    /**
     * Default locale for the store. This is the language and region code that
     * represents the default locale used in the store. It is required and
     * cannot be null.
     **/
    @Column(name = "default_locale", length = 10, nullable = false)
    private String defaultLocale;

    /**
     * Status of the store.
     **/
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    @Builder.Default
    private StoreStatus status = StoreStatus.PENDING; // Default changed to
                                                      // PENDING

    /**
     * The name of the database schema associated with this store. This is
     * required and must be unique across all stores.
     **/
    @Column(name = "schema_name", nullable = false, unique = true)
    private String schemaName;

    /**
     * The billing plan for the store. This determines the features and limits
     * available to the store. It is required and defaults to "BASIC".
     **/
    @Builder.Default
    @Column(name = "billing_plan", nullable = false)
    private String billingPlan = "BASIC";

    /**
     * Owner ID associated with the store.
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
     * JSON string containing various settings for the store. This can be used
     * to store custom configurations.
     **/
    @JdbcTypeCode(SqlTypes.JSON)
    private String settings; // Corresponds to 'configuration' from
                             // PlatformStores

    /**
     * Version number for optimistic locking. This is managed by JPA.
     **/
    @Version
    private Long version; // Changed from int to Long

    /**
     * Timestamp indicating when the store was created. This is set
     * automatically on creation and cannot be updated.
     **/
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Timestamp indicating when the store was last updated. This is set
     * automatically on creation and updated on every modification.
     **/
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
}
