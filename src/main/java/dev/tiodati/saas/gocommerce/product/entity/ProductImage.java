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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing a product image in the e-commerce system.
 *
 * <p>
 * Product images provide visual representation of products, supporting
 * multiple images per product with ordering and primary image designation.
 * </p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "product_image")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage extends PanacheEntityBase {

    /**
     * Unique identifier for the product image.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    /**
     * Product this image belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Current filename of the image.
     */
    @Column(name = "filename", nullable = false)
    private String filename;

    /**
     * Original filename when uploaded.
     */
    @Column(name = "original_filename")
    private String originalFilename;

    /**
     * File path where the image is stored.
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /**
     * File size in bytes.
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * MIME type of the image file.
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * Image width in pixels.
     */
    @Column(name = "width")
    private Integer width;

    /**
     * Image height in pixels.
     */
    @Column(name = "height")
    private Integer height;

    /**
     * Alternative text for accessibility.
     */
    @Column(name = "alt_text")
    private String altText;

    /**
     * Whether this is the primary/main image for the product.
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * Sort order for displaying images.
     */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * Timestamp when the image was created.
     */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the image was last updated.
     */
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

// Copilot: This file may have been generated or refactored by GitHub Copilot.
