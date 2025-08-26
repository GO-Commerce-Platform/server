package dev.tiodati.saas.gocommerce.promotion.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import dev.tiodati.saas.gocommerce.promotion.entity.PromotionConfig;

/**
 * Repository for promotion configuration operations.
 */
@ApplicationScoped
public class PromotionConfigRepository implements PanacheRepositoryBase<PromotionConfig, UUID> {

    /**
     * Finds all active promotions for a specific store.
     *
     * @param storeId The store ID
     * @return List of active promotion configurations
     */
    public List<PromotionConfig> findActiveByStore(UUID storeId) {
        return find("storeId = ?1 and active = true", storeId).list();
    }

    /**
     * Finds a promotion by code and store.
     *
     * @param storeId The store ID
     * @param code    The promotion code
     * @return Optional promotion configuration
     */
    public Optional<PromotionConfig> findByStoreAndCode(UUID storeId, String code) {
        return find("storeId = ?1 and code = ?2 and active = true", storeId, code).firstResultOptional();
    }

    /**
     * Finds volume-based promotions for a store, ordered by priority and minimum order amount.
     *
     * @param storeId The store ID
     * @return List of volume-based promotions ordered by applicability
     */
    public List<PromotionConfig> findVolumeBased(UUID storeId) {
        return find("storeId = ?1 and type = ?2 and active = true order by priority desc, minimumOrderAmount desc", 
                    storeId, PromotionConfig.PromotionType.VOLUME_BASED).list();
    }

    /**
     * Finds promotions by type for a store.
     *
     * @param storeId The store ID
     * @param type    The promotion type
     * @return List of promotions of the specified type
     */
    public List<PromotionConfig> findByStoreAndType(UUID storeId, PromotionConfig.PromotionType type) {
        return find("storeId = ?1 and type = ?2 and active = true", storeId, type).list();
    }

    /**
     * Finds all promotions for a store (including inactive ones).
     *
     * @param storeId The store ID
     * @return List of all promotion configurations
     */
    public List<PromotionConfig> findAllByStore(UUID storeId) {
        return find("storeId", storeId).list();
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
