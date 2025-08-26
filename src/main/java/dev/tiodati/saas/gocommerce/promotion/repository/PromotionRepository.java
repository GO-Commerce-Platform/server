package dev.tiodati.saas.gocommerce.promotion.repository;

import dev.tiodati.saas.gocommerce.promotion.entity.Promotion;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Promotion entities with tenant-aware operations.
 */
@ApplicationScoped
public class PromotionRepository implements PanacheRepository<Promotion> {

    /**
     * Find all active promotions for a specific store.
     */
    public List<Promotion> findActiveByStoreId(String storeId) {
        return list("storeId = ?1 and active = true", storeId);
    }

    /**
     * Find all active and valid promotions for a specific store.
     */
    public List<Promotion> findValidByStoreId(String storeId) {
        LocalDateTime now = LocalDateTime.now();
        return list("storeId = ?1 and active = true and validFrom <= ?2 and validUntil > ?2",
                   storeId, now);
    }

    /**
     * Find promotion by promo code for a specific store.
     */
    public Optional<Promotion> findByPromoCodeAndStoreId(String promoCode, String storeId) {
        return find("promoCode = ?1 and storeId = ?2", promoCode, storeId)
                .firstResultOptional();
    }

    /**
     * Find valid promotion by promo code for a specific store.
     */
    public Optional<Promotion> findValidByPromoCodeAndStoreId(String promoCode, String storeId) {
        LocalDateTime now = LocalDateTime.now();
        return find("promoCode = ?1 and storeId = ?2 and active = true and validFrom <= ?3 and validUntil > ?3",
                   promoCode, storeId, now)
                .firstResultOptional();
    }

    /**
     * Find promotions by store ID with pagination.
     */
    public List<Promotion> findByStoreId(String storeId, int page, int size) {
        return find("storeId = ?1", storeId)
                .page(page, size)
                .list();
    }

    /**
     * Count promotions by store ID.
     */
    public long countByStoreId(String storeId) {
        return count("storeId = ?1", storeId);
    }

    /**
     * Find expired promotions for cleanup.
     */
    public List<Promotion> findExpiredPromotions() {
        LocalDateTime now = LocalDateTime.now();
        return list("validUntil < ?1", now);
    }

    /**
     * Increment usage count for a promotion.
     */
    public void incrementUsageCount(Long promotionId) {
        update("usedCount = usedCount + 1 where id = ?1", promotionId);
    }
}
