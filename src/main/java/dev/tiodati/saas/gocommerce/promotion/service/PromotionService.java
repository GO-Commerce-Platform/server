package dev.tiodati.saas.gocommerce.promotion.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.promotion.entity.PromotionConfig;

/**
 * Service interface for promotion and discount management.
 */
public interface PromotionService {

    /**
     * Calculates the best discount for the given order based on promotion codes and automatic volume discounts.
     *
     * @param storeId         The store ID
     * @param orderAmount     The order amount
     * @param promotionCodes  List of promotion codes to apply
     * @return The best applicable discount amount
     */
    BigDecimal calculateBestDiscount(UUID storeId, BigDecimal orderAmount, List<String> promotionCodes);

    /**
     * Finds a promotion by code for a specific store.
     *
     * @param storeId The store ID
     * @param code    The promotion code
     * @return Optional promotion configuration
     */
    Optional<PromotionConfig> findPromotionByCode(UUID storeId, String code);

    /**
     * Finds all volume-based promotions for a store that apply to the given order amount.
     *
     * @param storeId     The store ID
     * @param orderAmount The order amount
     * @return List of applicable volume-based promotions
     */
    List<PromotionConfig> findApplicableVolumePromotions(UUID storeId, BigDecimal orderAmount);

    /**
     * Marks a promotion as used (increments usage count).
     *
     * @param promotionId The promotion ID
     * @return true if the promotion was successfully updated
     */
    boolean markPromotionUsed(UUID promotionId);

    /**
     * Creates a new promotion configuration.
     *
     * @param promotion The promotion configuration to create
     * @return The created promotion configuration
     */
    PromotionConfig createPromotion(PromotionConfig promotion);

    /**
     * Updates an existing promotion configuration.
     *
     * @param promotion The promotion configuration to update
     * @return The updated promotion configuration
     */
    PromotionConfig updatePromotion(PromotionConfig promotion);

    /**
     * Deactivates a promotion.
     *
     * @param promotionId The promotion ID
     * @return true if the promotion was successfully deactivated
     */
    boolean deactivatePromotion(UUID promotionId);

    /**
     * Gets all promotions for a store.
     *
     * @param storeId The store ID
     * @return List of all promotion configurations
     */
    List<PromotionConfig> getStorePromotions(UUID storeId);

    /**
     * Validates that a promotion can be applied.
     *
     * @param promotion   The promotion to validate
     * @param orderAmount The order amount
     * @return true if the promotion can be applied
     */
    boolean canApplyPromotion(PromotionConfig promotion, BigDecimal orderAmount);
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
