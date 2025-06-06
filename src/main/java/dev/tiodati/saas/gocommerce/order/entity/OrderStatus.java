package dev.tiodati.saas.gocommerce.order.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing order status definitions in the e-commerce system.
 *
 * <p>
 * This entity defines the various states an order can be in throughout its
 * lifecycle. Order statuses are predefined and control order workflow.
 * </p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "order_status")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatus extends PanacheEntityBase {

    /**
     * Unique identifier for the order status (e.g., "PENDING", "CONFIRMED").
     */
    @Id
    @Column(name = "id", length = 20)
    private String id;

    /**
     * Display name of the status.
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * Description of what this status means.
     */
    @Column(name = "description")
    private String description;

    /**
     * Whether this status is currently active/usable.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Sort order for displaying statuses.
     */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
