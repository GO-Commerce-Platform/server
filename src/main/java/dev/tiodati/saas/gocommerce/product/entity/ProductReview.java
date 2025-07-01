package dev.tiodati.saas.gocommerce.product.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing a product review in the e-commerce system.
 *
 * <p>
 * Product reviews allow customers to rate and comment on products they have
 * purchased, providing valuable feedback for other customers and store owners.
 * </p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "product_review")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReview extends PanacheEntityBase {

    /**
     * Enumeration for review status.
     */
    public enum ReviewStatus {
        /** Review is pending approval. */
        PENDING,
        /** Review has been approved. */
        APPROVED,
        /** Review has been rejected. */
        REJECTED
    }

    /**
     * Unique identifier for the product review.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * Product this review is for.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Customer ID who wrote the review.
     * Note: Using UUID for customer reference to avoid circular dependency.
     */
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /**
     * Rating given by the customer (1-5 stars).
     */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    /**
     * Title of the review.
     */
    @Column(name = "title")
    private String title;

    /**
     * Review text content.
     */
    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    /**
     * Current status of the review.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    /**
     * Whether this review is from a verified purchase.
     */
    @Column(name = "is_verified_purchase", nullable = false)
    @Builder.Default
    private Boolean isVerifiedPurchase = false;

    /**
     * Timestamp when the review was created.
     */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the review was last updated.
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
