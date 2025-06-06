package dev.tiodati.saas.gocommerce.order.entity;

import java.math.BigDecimal;
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
 * JPA entity representing an order header in the e-commerce system.
 *
 * <p>
 * Order headers contain the main order information including customer details,
 * totals, addresses, and status. Each order header can have multiple order
 * items.
 * </p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "order_header")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHeader extends PanacheEntityBase {

    /**
     * Unique identifier for the order.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    /**
     * Human-readable order number for customer reference.
     */
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    /**
     * Customer ID who placed the order.
     * Note: Using UUID for customer reference to avoid circular dependency.
     */
    @Column(name = "customer_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID customerId;

    /**
     * Current status of the order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", nullable = false)
    @Builder.Default
    private OrderStatus status = new OrderStatus(); // Will be set to "PENDING"

    /**
     * Subtotal before taxes and shipping.
     */
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    /**
     * Total tax amount.
     */
    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /**
     * Total shipping cost.
     */
    @Column(name = "shipping_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    /**
     * Total discount amount.
     */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Final total amount.
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Currency code for the order (ISO 4217).
     */
    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    /**
     * Locale for the order.
     */
    @Column(name = "locale", length = 5)
    @Builder.Default
    private String locale = "en";

    // Shipping Address Fields
    /**
     * Shipping address first name.
     */
    @Column(name = "shipping_first_name", length = 100)
    private String shippingFirstName;

    /**
     * Shipping address last name.
     */
    @Column(name = "shipping_last_name", length = 100)
    private String shippingLastName;

    /**
     * Shipping address line 1.
     */
    @Column(name = "shipping_address_line1")
    private String shippingAddressLine1;

    /**
     * Shipping address line 2.
     */
    @Column(name = "shipping_address_line2")
    private String shippingAddressLine2;

    /**
     * Shipping address city.
     */
    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    /**
     * Shipping address state/province.
     */
    @Column(name = "shipping_state_province", length = 100)
    private String shippingStateProvince;

    /**
     * Shipping address postal code.
     */
    @Column(name = "shipping_postal_code", length = 20)
    private String shippingPostalCode;

    /**
     * Shipping address country.
     */
    @Column(name = "shipping_country", length = 2)
    private String shippingCountry;

    /**
     * Shipping address phone.
     */
    @Column(name = "shipping_phone", length = 20)
    private String shippingPhone;

    // Billing Address Fields
    /**
     * Billing address first name.
     */
    @Column(name = "billing_first_name", length = 100)
    private String billingFirstName;

    /**
     * Billing address last name.
     */
    @Column(name = "billing_last_name", length = 100)
    private String billingLastName;

    /**
     * Billing address line 1.
     */
    @Column(name = "billing_address_line1")
    private String billingAddressLine1;

    /**
     * Billing address line 2.
     */
    @Column(name = "billing_address_line2")
    private String billingAddressLine2;

    /**
     * Billing address city.
     */
    @Column(name = "billing_city", length = 100)
    private String billingCity;

    /**
     * Billing address state/province.
     */
    @Column(name = "billing_state_province", length = 100)
    private String billingStateProvince;

    /**
     * Billing address postal code.
     */
    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;

    /**
     * Billing address country.
     */
    @Column(name = "billing_country", length = 2)
    private String billingCountry;

    /**
     * Billing address phone.
     */
    @Column(name = "billing_phone", length = 20)
    private String billingPhone;

    /**
     * When the order was placed.
     */
    @Column(name = "order_date", nullable = false)
    @Builder.Default
    private Instant orderDate = Instant.now();

    /**
     * When the order was shipped.
     */
    @Column(name = "shipped_date")
    private Instant shippedDate;

    /**
     * When the order was delivered.
     */
    @Column(name = "delivered_date")
    private Instant deliveredDate;

    /**
     * Additional notes for the order.
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Timestamp when the order was created.
     */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the order was last updated.
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
        if (orderDate == null) {
            orderDate = createdAt;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
