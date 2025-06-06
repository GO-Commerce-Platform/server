package dev.tiodati.saas.gocommerce.product.entity;

import java.math.BigDecimal;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing a product in the e-commerce system.
 *
 * <p>
 * Products are the core items sold in the store, containing pricing,
 * inventory, physical attributes, and SEO metadata. Each product
 * belongs to a category and has a lifecycle status.
 * </p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "product")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends PanacheEntityBase {

    /**
     * Unique identifier for the product.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    /**
     * Display name of the product.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * URL-friendly identifier for the product.
     */
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    /**
     * Detailed description of the product.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Short description for listings and summaries.
     */
    @Column(name = "short_description", length = 500)
    private String shortDescription;

    /**
     * Category this product belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Current selling price of the product.
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Original/comparison price for showing discounts.
     */
    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    /**
     * Cost price for profit calculations.
     */
    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    /**
     * Stock keeping unit - unique product identifier.
     */
    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    /**
     * Barcode for the product.
     */
    @Column(name = "barcode", length = 100)
    private String barcode;

    /**
     * Whether inventory tracking is enabled for this product.
     */
    @Column(name = "track_inventory", nullable = false)
    @Builder.Default
    private Boolean trackInventory = true;

    /**
     * Current inventory quantity.
     */
    @Column(name = "inventory_quantity", nullable = false)
    @Builder.Default
    private Integer inventoryQuantity = 0;

    /**
     * Low stock threshold for notifications.
     */
    @Column(name = "low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    /**
     * Product weight in kilograms.
     */
    @Column(name = "weight", precision = 8, scale = 3)
    private BigDecimal weight;

    /**
     * Product length in centimeters.
     */
    @Column(name = "length", precision = 8, scale = 2)
    private BigDecimal length;

    /**
     * Product width in centimeters.
     */
    @Column(name = "width", precision = 8, scale = 2)
    private BigDecimal width;

    /**
     * Product height in centimeters.
     */
    @Column(name = "height", precision = 8, scale = 2)
    private BigDecimal height;

    /**
     * SEO meta title for the product page.
     */
    @Column(name = "meta_title")
    private String metaTitle;

    /**
     * SEO meta description for the product page.
     */
    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    /**
     * SEO meta keywords for the product page.
     */
    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;

    /**
     * Current status of the product.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    /**
     * Whether the product is featured for promotional display.
     */
    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Whether the product requires shipping.
     */
    @Column(name = "requires_shipping", nullable = false)
    @Builder.Default
    private Boolean requiresShipping = true;

    /**
     * Whether the product is digital (downloadable).
     */
    @Column(name = "is_digital", nullable = false)
    @Builder.Default
    private Boolean isDigital = false;

    /**
     * Timestamp when the product was created.
     */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the product was last updated.
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
