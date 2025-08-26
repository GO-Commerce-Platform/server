package dev.tiodati.saas.gocommerce.promotion.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import dev.tiodati.saas.gocommerce.promotion.entity.PromotionConfig;
import dev.tiodati.saas.gocommerce.promotion.repository.PromotionConfigRepository;

/**
 * Implementation of promotion service with tenant-scoped discount management.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionConfigRepository promotionRepository;

    @Override
    public BigDecimal calculateBestDiscount(UUID storeId, BigDecimal orderAmount, List<String> promotionCodes) {
        Log.infof("Calculating best discount for store %s, order amount %.2f, codes: %s",
                storeId, orderAmount.doubleValue(), promotionCodes);

        BigDecimal bestDiscount = BigDecimal.ZERO;

        // 1. Check explicit promotion codes
        if (promotionCodes != null && !promotionCodes.isEmpty()) {
            for (String code : promotionCodes) {
                var promotion = findPromotionByCode(storeId, code);
                if (promotion.isPresent() && canApplyPromotion(promotion.get(), orderAmount)) {
                    var discount = promotion.get().calculateDiscount(orderAmount);
                    if (discount.compareTo(bestDiscount) > 0) {
                        bestDiscount = discount;
                        Log.infof("Found better discount from code '%s': %.2f", code, discount.doubleValue());
                    }
                }
            }
        }

        // 2. Check volume-based promotions (automatic discounts)
        var volumePromotions = findApplicableVolumePromotions(storeId, orderAmount);
        for (var promotion : volumePromotions) {
            if (canApplyPromotion(promotion, orderAmount)) {
                var discount = promotion.calculateDiscount(orderAmount);
                if (discount.compareTo(bestDiscount) > 0) {
                    bestDiscount = discount;
                    Log.infof("Found better volume discount '%s': %.2f", promotion.getName(), discount.doubleValue());
                }
            }
        }

        Log.infof("Best discount calculated: %.2f", bestDiscount.doubleValue());
        return bestDiscount;
    }

    @Override
    public Optional<PromotionConfig> findPromotionByCode(UUID storeId, String code) {
        return promotionRepository.findByStoreAndCode(storeId, code);
    }

    @Override
    public List<PromotionConfig> findApplicableVolumePromotions(UUID storeId, BigDecimal orderAmount) {
        return promotionRepository.findVolumeBased(storeId).stream()
                .filter(promo -> promo.qualifiesForOrderAmount(orderAmount))
                .sorted(Comparator.comparingInt(PromotionConfig::getPriority).reversed()
                        .thenComparing(promo -> promo.calculateDiscount(orderAmount), Comparator.reverseOrder()))
                .toList();
    }

    @Override
    @Transactional
    public boolean markPromotionUsed(UUID promotionId) {
        Log.infof("Marking promotion %s as used", promotionId);
        
        return promotionRepository.findByIdOptional(promotionId)
                .map(promotion -> {
                    promotion.setUsageCount(promotion.getUsageCount() + 1);
                    promotionRepository.persist(promotion);
                    Log.infof("Promotion %s usage count updated to %d", 
                            promotionId, promotion.getUsageCount());
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public PromotionConfig createPromotion(PromotionConfig promotion) {
        Log.infof("Creating promotion '%s' for store %s", promotion.getName(), promotion.getStoreId());
        
        promotionRepository.persist(promotion);
        return promotion;
    }

    @Override
    @Transactional
    public PromotionConfig updatePromotion(PromotionConfig promotion) {
        Log.infof("Updating promotion %s", promotion.getId());
        
        return promotionRepository.getEntityManager().merge(promotion);
    }

    @Override
    @Transactional
    public boolean deactivatePromotion(UUID promotionId) {
        Log.infof("Deactivating promotion %s", promotionId);
        
        return promotionRepository.findByIdOptional(promotionId)
                .map(promotion -> {
                    promotion.setActive(false);
                    promotionRepository.persist(promotion);
                    Log.infof("Promotion %s deactivated", promotionId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public List<PromotionConfig> getStorePromotions(UUID storeId) {
        return promotionRepository.findAllByStore(storeId);
    }

    @Override
    public boolean canApplyPromotion(PromotionConfig promotion, BigDecimal orderAmount) {
        if (!promotion.isCurrentlyValid()) {
            Log.debugf("Promotion %s is not currently valid", promotion.getCode());
            return false;
        }

        if (!promotion.qualifiesForOrderAmount(orderAmount)) {
            Log.debugf("Order amount %.2f does not qualify for promotion %s (min: %.2f)",
                    orderAmount.doubleValue(), promotion.getCode(),
                    promotion.getMinimumOrderAmount() != null ? promotion.getMinimumOrderAmount().doubleValue() : 0.0);
            return false;
        }

        return true;
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
