package dev.tiodati.saas.gocommerce.customer.entity;

import java.time.Instant;
import java.time.LocalDate;
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

/**
 * JPA entity representing a customer in the e-commerce system.
 *
 * <p>
 * Customers are registered users who can place orders, maintain profiles,
 * and have personalized shopping experiences. Each customer belongs to a
 * specific store in the multi-tenant architecture.
 * </p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "customer")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends PanacheEntityBase {

    /**
     * Enumeration for customer gender.
     */
    public enum Gender {
        /** Male gender. */
        MALE,
        /** Female gender. */
        FEMALE,
        /** Other gender. */
        OTHER,
        /** Prefer not to say. */
        PREFER_NOT_TO_SAY
    }

    /**
     * Unique identifier for the customer.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * Customer's email address - used for login and communication.
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * Customer's first name.
     */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * Customer's last name.
     */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Customer's phone number.
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Customer's date of birth.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Customer's gender.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "gender")
    private Gender gender;

    /**
     * Primary address line.
     */
    @Column(name = "address_line1")
    private String addressLine1;

    /**
     * Secondary address line.
     */
    @Column(name = "address_line2")
    private String addressLine2;

    /**
     * City name.
     */
    @Column(name = "city", length = 100)
    private String city;

    /**
     * State or province name.
     */
    @Column(name = "state_province", length = 100)
    private String stateProvince;

    /**
     * Postal/ZIP code.
     */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * Country code (ISO 3166-1 alpha-2).
     */
    @Column(name = "country", length = 2)
    private String country;

    /**
     * Current status of the customer account.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    /**
     * Whether the customer's email address is verified.
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    /**
     * Whether the customer opted in to marketing emails.
     */
    @Column(name = "marketing_emails_opt_in", nullable = false)
    @Builder.Default
    private Boolean marketingEmailsOptIn = false;

    /**
     * Customer's preferred language (ISO 639-1).
     */
    @Column(name = "preferred_language", length = 5)
    @Builder.Default
    private String preferredLanguage = "en";

    /**
     * Timestamp when the customer was created.
     */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the customer was last updated.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Version for optimistic locking.
     */
    @Version
    private Integer version;

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

// Copilot: This file may have been generated or refactored by GitHub Copilot.
