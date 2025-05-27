package dev.tiodati.saas.gocommerce.platform.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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
import lombok.EqualsAndHashCode; // Import EqualsAndHashCode
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a platform store entity. Each store is a tenant in the system.
 */
@Entity
@Table(name = "platform_stores")
@Data
@EqualsAndHashCode(callSuper = false) // Explicitly set callSuper to false
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStore extends PanacheEntityBase {

    /**
     * The unique identifier for the platform store.
     */
    @Id
    @GeneratedValue
    private UUID id; // Changed to private

    /**
     * The name of the store.
     */
    @Column(nullable = false, length = 100)
    private String name; // Changed to private

    /**
     * The subdomain for accessing the store (e.g., "my-store" in
     * "my-store.gocommerce.com"). This must be unique across all platform
     * stores.
     */
    @Column(nullable = false, unique = true, length = 63)
    private String subdomain; // Changed to private

    /**
     * The domain suffix for the store, typically the main platform domain.
     * Example: "gocommerce.com"
     */
    @Column(nullable = false, length = 100)
    private String domainSuffix; // Changed to private

    /**
     * The administrative email for the store.
     */
    @Column(length = 255)
    private String email; // Changed to private

    /**
     * The default currency code for the store (e.g., "USD", "EUR").
     */
    @Column(length = 3)
    private String currencyCode; // Changed to private

    /**
     * The default locale for the store (e.g., "en-US", "pt-BR").
     */
    @Column(length = 10)
    private String defaultLocale; // Changed to private

    /**
     * The current operational status of the store.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreStatus status; // Changed to private

    /**
     * A brief description of the store. This field can store a longer text
     * description for the store.
     */
    @Column(columnDefinition = "TEXT")
    private String description; // Added description field

    /**
     * Flag indicating if the store has been soft-deleted. True if deleted,
     * false otherwise.
     */
    @Column(nullable = false)
    private boolean deleted; // Changed to private

    /**
     * Timestamp of when the store entity was created.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Changed to private

    /**
     * Timestamp of when the store entity was last updated.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt; // Changed to private

    /**
     * Version number for optimistic locking.
     */
    @Version
    private Long version; // Changed to private

    // Helper method to construct the full domain
    /**
     * Gets the full domain name of the store (e.g., "my-store.gocommerce.com").
     *
     * @return The full domain name.
     */
    public String getFullDomain() {
        if (subdomain == null || domainSuffix == null) {
            return null;
        }
        return subdomain + "." + domainSuffix;
    }
}
