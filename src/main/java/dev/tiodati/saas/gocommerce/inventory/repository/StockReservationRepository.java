package dev.tiodati.saas.gocommerce.inventory.repository;

import dev.tiodati.saas.gocommerce.inventory.entity.StockReservation;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for StockReservation entity operations.
 * Provides database access methods for persistent stock reservation management.
 */
@ApplicationScoped
public class StockReservationRepository implements PanacheRepositoryBase<StockReservation, String> {

    /**
     * Find an active reservation by its ID.
     * Only returns reservations that are still active (not expired, confirmed, or released).
     *
     * @param reservationId the reservation ID
     * @return optional containing the active reservation if found
     */
    public Optional<StockReservation> findActiveReservation(String reservationId) {
        return find("reservationId = ?1 AND status = ?2", 
                    reservationId, StockReservation.ReservationStatus.ACTIVE)
                .firstResultOptional();
    }

    /**
     * Find all active reservations for a specific product.
     * This is used to calculate available stock considering reservations.
     *
     * @param productId the product ID
     * @return list of active reservations for the product
     */
    public List<StockReservation> findActiveReservationsByProduct(UUID productId) {
        return find("productId = ?1 AND status = ?2 AND expiresAt > ?3", 
                    productId, StockReservation.ReservationStatus.ACTIVE, Instant.now())
                .list();
    }

    /**
     * Calculate total reserved quantity for a product.
     * Excludes expired reservations automatically.
     *
     * @param productId the product ID
     * @return total quantity currently reserved for this product
     */
    @SuppressWarnings("unchecked")
    public int getTotalReservedQuantity(UUID productId) {
        String query = """
            SELECT COALESCE(SUM(quantity), 0) 
            FROM StockReservation 
            WHERE productId = ?1 AND status = ?2 AND expiresAt > ?3
            """;
        
        var result = getEntityManager().createQuery(query, Long.class)
                .setParameter(1, productId)
                .setParameter(2, StockReservation.ReservationStatus.ACTIVE)
                .setParameter(3, Instant.now())
                .getSingleResult();
        
        return result != null ? result.intValue() : 0;
    }

    /**
     * Find reservations that have expired and can be cleaned up.
     *
     * @param page pagination information
     * @return list of expired reservations
     */
    public List<StockReservation> findExpiredReservations(Page page) {
        return find("status = ?1 AND expiresAt <= ?2", 
                    StockReservation.ReservationStatus.ACTIVE, Instant.now())
                .page(page)
                .list();
    }

    /**
     * Find reservations created by a specific user/system.
     *
     * @param reservedBy the user or system identifier
     * @param page pagination information
     * @return list of reservations created by the specified user/system
     */
    public List<StockReservation> findByReservedBy(String reservedBy, Page page) {
        return find("reservedBy = ?1 ORDER BY reservedAt DESC", reservedBy)
                .page(page)
                .list();
    }

    /**
     * Find reservations by reference (order ID, session ID, etc.).
     *
     * @param reference the reference identifier
     * @return list of reservations with the specified reference
     */
    public List<StockReservation> findByReference(String reference) {
        return find("reference = ?1 ORDER BY reservedAt DESC", reference).list();
    }

    /**
     * Count active reservations for a product.
     *
     * @param productId the product ID
     * @return number of active reservations for the product
     */
    public long countActiveReservationsByProduct(UUID productId) {
        return count("productId = ?1 AND status = ?2 AND expiresAt > ?3", 
                     productId, StockReservation.ReservationStatus.ACTIVE, Instant.now());
    }

    /**
     * Create a new stock reservation with validation.
     *
     * @param reservationId unique identifier for the reservation
     * @param productId product being reserved
     * @param quantity quantity to reserve
     * @param expiryMinutes minutes until expiry (default 15 if null)
     * @param reservedBy user or system creating the reservation
     * @param reference optional reference identifier
     * @param notes optional notes
     * @return the created reservation
     */
    @Transactional
    public StockReservation createReservation(String reservationId, UUID productId, Integer quantity,
                                            Integer expiryMinutes, String reservedBy, String reference, String notes) {
        // Check if reservation already exists
        if (findByIdOptional(reservationId).isPresent()) {
            throw new IllegalArgumentException("Reservation ID already exists: " + reservationId);
        }

        int expiryMins = expiryMinutes != null ? expiryMinutes : 15;
        Instant expiresAt = Instant.now().plusSeconds(expiryMins * 60L);

        StockReservation reservation = StockReservation.builder()
                .reservationId(reservationId)
                .productId(productId)
                .quantity(quantity)
                .status(StockReservation.ReservationStatus.ACTIVE)
                .expiresAt(expiresAt)
                .reservedBy(reservedBy)
                .reference(reference)
                .notes(notes)
                .build();

        persist(reservation);
        return reservation;
    }

    /**
     * Update reservation status and persist changes.
     *
     * @param reservationId the reservation ID
     * @param status new status
     * @return true if reservation was found and updated
     */
    @Transactional
    public boolean updateReservationStatus(String reservationId, StockReservation.ReservationStatus status) {
        Optional<StockReservation> reservationOpt = findByIdOptional(reservationId);
        if (reservationOpt.isPresent()) {
            StockReservation reservation = reservationOpt.get();
            reservation.setStatus(status);
            reservation.setUpdatedAt(Instant.now());
            persist(reservation);
            return true;
        }
        return false;
    }

    /**
     * Extend the expiry time of an active reservation.
     *
     * @param reservationId the reservation ID
     * @param additionalMinutes additional minutes to add to expiry
     * @return true if reservation was found and extended
     */
    @Transactional
    public boolean extendReservation(String reservationId, int additionalMinutes) {
        Optional<StockReservation> reservationOpt = findActiveReservation(reservationId);
        if (reservationOpt.isPresent()) {
            StockReservation reservation = reservationOpt.get();
            reservation.extendExpiry(additionalMinutes * 60L);
            persist(reservation);
            return true;
        }
        return false;
    }

    /**
     * Clean up expired reservations by marking them as EXPIRED.
     * This should be called periodically to prevent expired reservations from accumulating.
     *
     * @param batchSize maximum number of reservations to process in one batch
     * @return number of reservations that were expired
     */
    @Transactional
    public int expireOldReservations(int batchSize) {
        List<StockReservation> expiredReservations = findExpiredReservations(Page.of(0, batchSize));
        
        for (StockReservation reservation : expiredReservations) {
            reservation.expire();
            persist(reservation);
        }
        
        return expiredReservations.size();
    }

    /**
     * Delete old reservation records that are no longer needed.
     * Removes EXPIRED, CONFIRMED, and RELEASED reservations older than the specified age.
     *
     * @param olderThanDays delete reservations older than this many days
     * @return number of records deleted
     */
    @Transactional
    public long deleteOldReservations(int olderThanDays) {
        Instant cutoffTime = Instant.now().minusSeconds(olderThanDays * 24 * 60 * 60L);
        
        return delete("status IN (?1, ?2, ?3) AND updatedAt < ?4",
                     StockReservation.ReservationStatus.EXPIRED,
                     StockReservation.ReservationStatus.CONFIRMED,
                     StockReservation.ReservationStatus.RELEASED,
                     cutoffTime);
    }

    /**
     * Get reservation statistics for monitoring and reporting.
     *
     * @return reservation statistics
     */
    @SuppressWarnings("unchecked")
    public ReservationStats getReservationStats() {
        String query = """
            SELECT 
                COUNT(*) as total,
                COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active,
                COUNT(CASE WHEN status = 'ACTIVE' AND expiresAt <= :now THEN 1 END) as expired,
                COUNT(CASE WHEN status = 'CONFIRMED' THEN 1 END) as confirmed,
                COUNT(CASE WHEN status = 'RELEASED' THEN 1 END) as released
            FROM StockReservation
            """;
        
        Object[] result = (Object[]) getEntityManager().createQuery(query)
                .setParameter("now", Instant.now())
                .getSingleResult();
        
        return new ReservationStats(
            ((Number) result[0]).longValue(),
            ((Number) result[1]).longValue(),
            ((Number) result[2]).longValue(),
            ((Number) result[3]).longValue(),
            ((Number) result[4]).longValue()
        );
    }

    /**
     * Statistics about stock reservations.
     */
    public record ReservationStats(
        long totalReservations,
        long activeReservations,
        long expiredReservations,
        long confirmedReservations,
        long releasedReservations
    ) {}
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
