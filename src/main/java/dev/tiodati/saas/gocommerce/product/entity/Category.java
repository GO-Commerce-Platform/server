package dev.tiodati.saas.gocommerce.product.entity;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing a product category in the e-commerce system.
 *
 * <p>
 * Categories are used to organize products hierarchically, supporting nested
 * category structures with parent-child relationships. Each category can have
 * SEO metadata and visibility controls.
 * </p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "category")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends PanacheEntityBase {

    /**
     * Unique identifier for the category.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    /**
     * Display name of the category.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * URL-friendly identifier for the category.
     */
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    /**
     * Detailed description of the category.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Parent category for hierarchical organization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /**
     * Sort order for displaying categories.
     */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * SEO meta title for the category page.
     */
    @Column(name = "meta_title")
    private String metaTitle;

    /**
     * SEO meta description for the category page.
     */
    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    /**
     * SEO meta keywords for the category page.
     */
    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;

    /**
     * Whether the category is active and visible to customers.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Whether the category is featured for promotional display.
     */
    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Timestamp when the category was created.
     */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the category was last updated.
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
