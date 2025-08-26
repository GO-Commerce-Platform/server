package dev.tiodati.saas.gocommerce.promotion.service;

import dev.tiodati.saas.gocommerce.promotion.entity.PromotionConfig;
import dev.tiodati.saas.gocommerce.promotion.repository.PromotionConfigRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class PromotionServiceImplTest {

    @Inject
    PromotionServiceImpl promotionService;

    @InjectMock
    PromotionConfigRepository promotionRepository;

    private UUID storeId;
    private PromotionConfig percentagePromotion;
    private PromotionConfig fixedAmountPromotion;
    private PromotionConfig volumePromotion;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();

        // Create test promotions
        percentagePromotion = PromotionConfig.builder()
                .id(UUID.randomUUID())
                .storeId(storeId)
                .code("SAVE10")
                .name("10% Off")
                .type(PromotionConfig.PromotionType.PERCENTAGE)
                .discountValue(new BigDecimal("0.10")) // 10%
                .minimumOrderAmount(new BigDecimal("50.00"))
                .maximumDiscountAmount(new BigDecimal("25.00"))
                .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                .active(true)
                .priority(10)
                .usageCount(0)
                .build();

        fixedAmountPromotion = PromotionConfig.builder()
                .id(UUID.randomUUID())
                .storeId(storeId)
                .code("WELCOME5")
                .name("$5 Off")
                .type(PromotionConfig.PromotionType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("5.00"))
                .minimumOrderAmount(new BigDecimal("25.00"))
                .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                .active(true)
                .priority(5)
                .usageCount(0)
                .build();

        volumePromotion = PromotionConfig.builder()
                .id(UUID.randomUUID())
                .storeId(storeId)
                .code("VOLUME100")
                .name("15% Off $100+")
                .type(PromotionConfig.PromotionType.VOLUME_BASED)
                .discountValue(new BigDecimal("0.15")) // 15%
                .minimumOrderAmount(new BigDecimal("100.00"))
                .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                .active(true)
                .priority(15)
                .usageCount(0)
                .build();
    }

    @Test
    @DisplayName("Should find promotion by code")
    void testFindPromotionByCode() {
        // Given
        when(promotionRepository.findByStoreAndCode(storeId, "SAVE10"))
                .thenReturn(Optional.of(percentagePromotion));

        // When
        var result = promotionService.findPromotionByCode(storeId, "SAVE10");

        // Then
        assertTrue(result.isPresent());
        assertEquals(percentagePromotion.getId(), result.get().getId());
        assertEquals("SAVE10", result.get().getCode());
    }

    @Test
    @DisplayName("Should return empty when promotion code not found")
    void testFindPromotionByCode_NotFound() {
        // Given
        when(promotionRepository.findByStoreAndCode(storeId, "NONEXISTENT"))
                .thenReturn(Optional.empty());

        // When
        var result = promotionService.findPromotionByCode(storeId, "NONEXISTENT");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should calculate best discount from multiple promotions")
    void testCalculateBestDiscount_MultiplePromotions() {
        // Given - order amount $120.00 qualifies for all promotions
        var orderAmount = new BigDecimal("120.00");
        var promotionCodes = List.of("SAVE10", "WELCOME5");

        when(promotionRepository.findByStoreAndCode(storeId, "SAVE10"))
                .thenReturn(Optional.of(percentagePromotion));
        when(promotionRepository.findByStoreAndCode(storeId, "WELCOME5"))
                .thenReturn(Optional.of(fixedAmountPromotion));
        when(promotionRepository.findVolumeBased(storeId))
                .thenReturn(List.of(volumePromotion));

        // When
        var bestDiscount = promotionService.calculateBestDiscount(storeId, orderAmount, promotionCodes);

        // Then
        // Volume promotion gives 15% of $120 = $18.00 (best)
        // Percentage promotion gives 10% of $120 = $12.00 (but capped at $25)
        // Fixed amount promotion gives $5.00
        assertEquals(new BigDecimal("18.00"), bestDiscount);
    }

    @Test
    @DisplayName("Should calculate percentage discount with maximum cap")
    void testCalculateBestDiscount_PercentageWithCap() {
        // Given - large order that would exceed maximum discount
        var orderAmount = new BigDecimal("500.00");
        var promotionCodes = List.of("SAVE10");

        when(promotionRepository.findByStoreAndCode(storeId, "SAVE10"))
                .thenReturn(Optional.of(percentagePromotion));
        when(promotionRepository.findVolumeBased(storeId))
                .thenReturn(List.of());

        // When
        var bestDiscount = promotionService.calculateBestDiscount(storeId, orderAmount, promotionCodes);

        // Then
        // 10% of $500 = $50, but capped at $25
        assertEquals(new BigDecimal("25.00"), bestDiscount);
    }

    @Test
    @DisplayName("Should return zero discount when order doesn't meet minimum")
    void testCalculateBestDiscount_BelowMinimum() {
        // Given - order amount below minimum for all promotions
        var orderAmount = new BigDecimal("20.00");
        var promotionCodes = List.of("SAVE10", "WELCOME5");

        when(promotionRepository.findByStoreAndCode(storeId, "SAVE10"))
                .thenReturn(Optional.of(percentagePromotion));
        when(promotionRepository.findByStoreAndCode(storeId, "WELCOME5"))
                .thenReturn(Optional.of(fixedAmountPromotion));
        when(promotionRepository.findVolumeBased(storeId))
                .thenReturn(List.of(volumePromotion));

        // When
        var bestDiscount = promotionService.calculateBestDiscount(storeId, orderAmount, promotionCodes);

        // Then
        assertEquals(BigDecimal.ZERO, bestDiscount);
    }

    @Test
    @DisplayName("Should apply volume-based discount automatically")
    void testCalculateBestDiscount_VolumeBasedOnly() {
        // Given - no promotion codes, but order qualifies for volume discount
        var orderAmount = new BigDecimal("150.00");
        var promotionCodes = List.<String>of(); // Empty codes

        when(promotionRepository.findVolumeBased(storeId))
                .thenReturn(List.of(volumePromotion));

        // When
        var bestDiscount = promotionService.calculateBestDiscount(storeId, orderAmount, promotionCodes);

        // Then
        // Volume promotion gives 15% of $150 = $22.50
        assertEquals(new BigDecimal("22.50"), bestDiscount);
    }

    @Test
    @DisplayName("Should find applicable volume promotions")
    void testFindApplicableVolumePromotions() {
        // Given
        var orderAmount = new BigDecimal("150.00");
        when(promotionRepository.findVolumeBased(storeId))
                .thenReturn(List.of(volumePromotion));

        // When
        var applicablePromotions = promotionService.findApplicableVolumePromotions(storeId, orderAmount);

        // Then
        assertEquals(1, applicablePromotions.size());
        assertEquals(volumePromotion.getId(), applicablePromotions.get(0).getId());
    }

    @Test
    @DisplayName("Should create new promotion")
    void testCreatePromotion() {
        // Given
        var newPromotion = PromotionConfig.builder()
                .storeId(storeId)
                .code("NEWCODE")
                .name("New Promotion")
                .type(PromotionConfig.PromotionType.PERCENTAGE)
                .discountValue(new BigDecimal("0.20"))
                .validFrom(Instant.now())
                .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                .active(true)
                .build();

        doAnswer(invocation -> {
            PromotionConfig promotion = invocation.getArgument(0);
            promotion.setId(UUID.randomUUID()); // Simulate database ID assignment
            return null;
        }).when(promotionRepository).persist(any(PromotionConfig.class));

        // When
        var result = promotionService.createPromotion(newPromotion);

        // Then
        assertNotNull(result);
        assertEquals("NEWCODE", result.getCode());
        assertEquals("New Promotion", result.getName());
        verify(promotionRepository).persist(newPromotion);
    }

    @Test
    @DisplayName("Should mark promotion as used")
    void testMarkPromotionUsed() {
        // Given
        when(promotionRepository.findByIdOptional(percentagePromotion.getId()))
                .thenReturn(Optional.of(percentagePromotion));

        // When
        var result = promotionService.markPromotionUsed(percentagePromotion.getId());

        // Then
        assertTrue(result);
        assertEquals(1, percentagePromotion.getUsageCount());
        verify(promotionRepository).persist(percentagePromotion);
    }

    @Test
    @DisplayName("Should not mark non-existent promotion as used")
    void testMarkPromotionUsed_NotFound() {
        // Given
        var nonExistentId = UUID.randomUUID();
        when(promotionRepository.findByIdOptional(nonExistentId))
                .thenReturn(Optional.empty());

        // When
        var result = promotionService.markPromotionUsed(nonExistentId);

        // Then
        assertFalse(result);
        verify(promotionRepository, never()).persist(any(PromotionConfig.class));
    }

    @Test
    @DisplayName("Should deactivate promotion")
    void testDeactivatePromotion() {
        // Given
        when(promotionRepository.findByIdOptional(percentagePromotion.getId()))
                .thenReturn(Optional.of(percentagePromotion));

        // When
        var result = promotionService.deactivatePromotion(percentagePromotion.getId());

        // Then
        assertTrue(result);
        assertFalse(percentagePromotion.getActive());
        verify(promotionRepository).persist(percentagePromotion);
    }

    @Test
    @DisplayName("Should get all store promotions")
    void testGetStorePromotions() {
        // Given
        var allPromotions = List.of(percentagePromotion, fixedAmountPromotion, volumePromotion);
        when(promotionRepository.findAllByStore(storeId))
                .thenReturn(allPromotions);

        // When
        var result = promotionService.getStorePromotions(storeId);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains(percentagePromotion));
        assertTrue(result.contains(fixedAmountPromotion));
        assertTrue(result.contains(volumePromotion));
    }

    @Test
    @DisplayName("Should validate promotion applicability")
    void testCanApplyPromotion() {
        // Given - valid promotion and qualifying order
        var orderAmount = new BigDecimal("75.00");

        // When
        var canApply = promotionService.canApplyPromotion(percentagePromotion, orderAmount);

        // Then
        assertTrue(canApply);
    }

    @Test
    @DisplayName("Should reject expired promotion")
    void testCanApplyPromotion_Expired() {
        // Given - expired promotion
        var expiredPromotion = PromotionConfig.builder()
                .id(UUID.randomUUID())
                .storeId(storeId)
                .code("EXPIRED")
                .name("Expired Promotion")
                .type(PromotionConfig.PromotionType.PERCENTAGE)
                .discountValue(new BigDecimal("0.10"))
                .validFrom(Instant.now().minus(30, ChronoUnit.DAYS))
                .validUntil(Instant.now().minus(1, ChronoUnit.DAYS)) // Expired yesterday
                .active(true)
                .usageCount(0)
                .build();
        
        var orderAmount = new BigDecimal("75.00");

        // When
        var canApply = promotionService.canApplyPromotion(expiredPromotion, orderAmount);

        // Then
        assertFalse(canApply);
    }

    @Test
    @DisplayName("Should reject promotion for order below minimum")
    void testCanApplyPromotion_BelowMinimum() {
        // Given - order amount below promotion minimum
        var orderAmount = new BigDecimal("30.00"); // Below $50 minimum

        // When
        var canApply = promotionService.canApplyPromotion(percentagePromotion, orderAmount);

        // Then
        assertFalse(canApply);
    }
}
