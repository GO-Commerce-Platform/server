package dev.tiodati.saas.gocommerce.inventory.entity;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity for tracking temporary stock reservations in a multi-tenant environment.
 * 
 * <p>
 * Stock reservations are used during order processing to temporarily hold inventory
 * while payment processing or other order completion steps occur. Reservations
 * have a configurable expiry time to prevent abandoned reservations from
 * permanently blocking stock availability.
 * </p>
 * 
 * <p>
 * This entity supports the complete order workflow:
 * <ol>
 *   <li>Reserve stock during cart checkout</li>
 *   <li>Confirm reservation when payment succeeds (converts to actual inventory reduction)</li>
 *   <li>Release reservation when payment fails or user abandons cart</li>
 *   <li>Auto-expire reservations that are never confirmed or released</li>
 * </ol>
 * </p>
 */
@Entity
@Table(name = "stock_reservation")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation extends PanacheEntityBase {

    /**
     * Unique identifier for the reservation.
     * This is typically provided by the calling system (e.g., order ID, session ID).
     */
    @Id
    @Column(name = "reservation_id")
    private String reservationId;

    /**
     * Product being reserved.
     */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /**
     * Quantity of stock reserved.
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Current status of the reservation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.ACTIVE;

    /**
     * When the reservation was created.
     */
    @Column(name = "reserved_at", nullable = false, updatable = false)
    private Instant reservedAt;

    /**
     * When the reservation expires and can be automatically cleaned up.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * User or system that created the reservation.
     */
    @Column(name = "reserved_by")
    private String reservedBy;

    /**
     * Optional reference to the order, session, or other business context.
     */
    @Column(name = "reference")
    private String reference;

    /**
     * Additional notes about the reservation.
     */
    @Column(name = "notes")
    private String notes;

    /**
     * When the reservation was last updated.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Status of a stock reservation.
     */
    public enum ReservationStatus {
        /** Reservation is active and blocking stock */
        ACTIVE,
        /** Reservation was confirmed and converted to actual stock reduction */
        CONFIRMED,
        /** Reservation was released and stock is available again */
        RELEASED,
        /** Reservation expired and was automatically cleaned up */
        EXPIRED
    }

    @PrePersist
    void prePersist() {
        if (reservedAt == null) {
            reservedAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        // Default expiry: 15 minutes from creation
        if (expiresAt == null) {
            expiresAt = reservedAt.plusSeconds(15 * 60);
        }
    }

    /**
     * Checks if this reservation is expired based on current time.
     * 
     * @return true if the reservation has passed its expiry time
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if this reservation is active (not expired, confirmed, or released).
     * 
     * @return true if the reservation is still active
     */
    public boolean isActive() {
        return status == ReservationStatus.ACTIVE && !isExpired();
    }

    /**
     * Marks this reservation as confirmed.
     */
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks this reservation as released.
     */
    public void release() {
        this.status = ReservationStatus.RELEASED;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks this reservation as expired.
     */
    public void expire() {
        this.status = ReservationStatus.EXPIRED;
        this.updatedAt = Instant.now();
    }

    /**
     * Gets remaining time until expiry in seconds.
     * 
     * @return seconds until expiry, or 0 if already expired
     */
    public long getSecondsUntilExpiry() {
        if (isExpired()) {
            return 0;
        }
        return Instant.now().until(expiresAt, java.time.temporal.ChronoUnit.SECONDS);
    }

    /**
     * Updates the expiry time to extend the reservation.
     * 
     * @param additionalSeconds additional seconds to extend the reservation
     */
    public void extendExpiry(long additionalSeconds) {
        if (status == ReservationStatus.ACTIVE) {
            this.expiresAt = this.expiresAt.plusSeconds(additionalSeconds);
            this.updatedAt = Instant.now();
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
