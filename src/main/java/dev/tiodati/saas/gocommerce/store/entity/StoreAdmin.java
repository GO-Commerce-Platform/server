package dev.tiodati.saas.gocommerce.store.entity;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "store_admin")
@Getter
@Setter
@NoArgsConstructor
public class StoreAdmin extends PanacheEntityBase {

    /**
     * Unique identifier for the store admin. This is a UUID that is generated
     * when the store admin is created. It serves as the primary key for the
     * store admin entity.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * The store to which this admin belongs. This is a many-to-one relationship
     * with the Store entity. It is required and cannot be null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /**
     * Username for the store admin. This is a unique identifier that is used
     * to log in to the store admin panel. It is required and must be unique
     * across all store admins.
     */
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /**
     * Email address for the store admin. This is used for communication and
     * password recovery. It is required and must be unique across all store admins.
     */
    @Column(nullable = false, unique = true)
    private String email;


    /**
     * First name of the store admin. This is used for personalization and
     * communication. It is required and cannot be null.
     */
    @Column(name = "first_name", nullable = false)
    private String firstName;

    /**
     * Last name of the store admin. This is used for personalization and
     * communication. It is required and cannot be null.
     */
    @Column(name = "last_name", nullable = false)
    private String lastName;

    /**
     * Status of the store admin. This indicates whether the admin is active,
     * suspended, or deleted. It defaults to ACTIVE.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreAdminStatus status = StoreAdminStatus.ACTIVE;

    /**
     * Indicates whether the store admin is deleted. This is a soft delete flag
     * that allows for logical deletion without removing the record from the database.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    /**
     * Version for optimistic locking. This is used to prevent concurrent updates
     * from overwriting each other. It is automatically managed by JPA.
     */
    @Version
    private int version;

    /**
     * Timestamp when the store admin was created. This is automatically set
     * when the store admin is created and cannot be changed.
     */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the store admin was last updated. This is automatically
     * updated whenever the store admin entity is modified.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Pre-persist lifecycle method to set the createdAt and updatedAt timestamps
     * before the entity is persisted to the database.
     */
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
