package dev.tiodati.saas.gocommerce.inventory.service;

import dev.tiodati.saas.gocommerce.inventory.dto.InventoryAdjustmentDto;
import dev.tiodati.saas.gocommerce.inventory.dto.InventoryReportDto;
import dev.tiodati.saas.gocommerce.inventory.dto.InventoryUpdateDto;
import dev.tiodati.saas.gocommerce.inventory.dto.LowStockAlertDto;
import dev.tiodati.saas.gocommerce.inventory.entity.InventoryAdjustment;
import dev.tiodati.saas.gocommerce.inventory.repository.InventoryAdjustmentRepository;
import dev.tiodati.saas.gocommerce.product.entity.Product;
import dev.tiodati.saas.gocommerce.product.entity.ProductStatus;
import dev.tiodati.saas.gocommerce.product.repository.ProductRepository;

import io.quarkus.logging.Log;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of InventoryService providing comprehensive inventory management.
 * Handles stock tracking, adjustments, reporting, and stock reservations.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;

    @Inject
    SecurityIdentity securityIdentity;

    // In-memory stock reservations (in production, use Redis or database)
    private final Map<String, StockReservation> stockReservations = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public boolean updateProductInventory(UUID storeId, UUID productId, InventoryUpdateDto updateDto) {
        Log.infof("Updating inventory for product %s in store %s: quantity=%d", 
                  productId, storeId, updateDto.newQuantity());

        try {
            Optional<Product> productOpt = productRepository.findByIdOptional(productId);
            if (productOpt.isEmpty()) {
                Log.warnf("Product not found: %s", productId);
                return false;
            }

            Product product = productOpt.get();
            
            // Validate business rules
            if (!validateStockUpdate(product, updateDto)) {
                return false;
            }

            Integer previousQuantity = product.getInventoryQuantity();
            
            // Update stock quantity
            product.setInventoryQuantity(updateDto.newQuantity());
            
            // Update low stock threshold if provided
            if (updateDto.lowStockThreshold() != null) {
                product.setLowStockThreshold(updateDto.lowStockThreshold());
            }

            productRepository.persist(product);

            // Create audit record
            InventoryAdjustment adjustment = createAdjustment(
                productId, 
                InventoryAdjustment.AdjustmentType.SET,
                updateDto.newQuantity(),
                previousQuantity,
                updateDto.newQuantity(),
                updateDto.reason() != null ? updateDto.reason() : "Stock level update",
                null,
                null
            );
            
            adjustmentRepository.persist(adjustment);

            Log.infof("Inventory updated successfully for product %s: %d -> %d", 
                      productId, previousQuantity, updateDto.newQuantity());
            return true;

        } catch (Exception e) {
            Log.errorf(e, "Failed to update inventory for product %s", productId);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean recordInventoryAdjustment(UUID storeId, InventoryAdjustmentDto adjustmentDto) {
        Log.infof("Recording inventory adjustment for product %s: type=%s, quantity=%d", 
                  adjustmentDto.productId(), adjustmentDto.adjustmentType(), adjustmentDto.quantity());

        try {
            Optional<Product> productOpt = productRepository.findByIdOptional(adjustmentDto.productId());
            if (productOpt.isEmpty()) {
                Log.warnf("Product not found: %s", adjustmentDto.productId());
                return false;
            }

            Product product = productOpt.get();
            Integer previousQuantity = product.getInventoryQuantity();
            Integer newQuantity = calculateNewQuantity(previousQuantity, adjustmentDto);

            // Validate the adjustment
            if (newQuantity < 0) {
                Log.warnf("Adjustment would result in negative stock: %s (current: %d, adjustment: %s %d)", 
                         adjustmentDto.productId(), previousQuantity, adjustmentDto.adjustmentType(), adjustmentDto.quantity());
                return false;
            }

            // Apply the adjustment
            product.setInventoryQuantity(newQuantity);
            productRepository.persist(product);

            // Create audit record
            InventoryAdjustment adjustment = createAdjustment(
                adjustmentDto.productId(),
                mapAdjustmentType(adjustmentDto.adjustmentType()),
                adjustmentDto.quantity(),
                previousQuantity,
                newQuantity,
                adjustmentDto.reason(),
                adjustmentDto.reference(),
                adjustmentDto.notes()
            );
            
            adjustmentRepository.persist(adjustment);

            Log.infof("Inventory adjustment recorded for product %s: %d -> %d (%s)", 
                      adjustmentDto.productId(), previousQuantity, newQuantity, adjustmentDto.adjustmentType());
            return true;

        } catch (Exception e) {
            Log.errorf(e, "Failed to record inventory adjustment for product %s", adjustmentDto.productId());
            return false;
        }
    }

    @Override
    public List<LowStockAlertDto> getLowStockAlerts(UUID storeId, Integer limit, LowStockAlertDto.UrgencyLevel urgencyLevel) {
        Log.infof("Getting low stock alerts for store %s (limit=%s, urgency=%s)", 
                  storeId, limit, urgencyLevel);

        List<Product> products = productRepository.find("status = ?1 AND trackInventory = true", ProductStatus.ACTIVE).list();
        
        List<LowStockAlertDto> alerts = products.stream()
                .filter(this::isLowStock)
                .map(this::mapToLowStockAlert)
                .filter(alert -> urgencyLevel == null || alert.getUrgencyLevel() == urgencyLevel)
                .sorted((a, b) -> {
                    // Sort by urgency (CRITICAL first), then by stock percentage (lowest first)
                    int urgencyComparison = a.getUrgencyLevel().compareTo(b.getUrgencyLevel());
                    if (urgencyComparison != 0) {
                        return urgencyComparison;
                    }
                    return Integer.compare(a.getStockPercentage(), b.getStockPercentage());
                })
                .limit(limit != null ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());

        Log.infof("Found %d low stock alerts for store %s", alerts.size(), storeId);
        return alerts;
    }

    @Override
    public InventoryReportDto generateInventoryReport(UUID storeId, InventoryReportDto.ReportType reportType, boolean includeCategoryBreakdown) {
        Log.infof("Generating inventory report for store %s: type=%s", storeId, reportType);

        List<Product> allProducts = productRepository.find("status = ?1", ProductStatus.ACTIVE).list();
        
        // Calculate basic statistics
        int totalProducts = allProducts.size();
        int totalStockUnits = allProducts.stream()
                .filter(p -> p.getTrackInventory())
                .mapToInt(p -> p.getInventoryQuantity() != null ? p.getInventoryQuantity() : 0)
                .sum();
        
        BigDecimal totalStockValue = allProducts.stream()
                .filter(p -> p.getTrackInventory() && p.getInventoryQuantity() != null && p.getCostPrice() != null)
                .map(p -> p.getCostPrice().multiply(BigDecimal.valueOf(p.getInventoryQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int lowStockCount = (int) allProducts.stream()
                .filter(this::isLowStock)
                .count();

        int outOfStockCount = (int) allProducts.stream()
                .filter(this::isOutOfStock)
                .count();

        double averageStockLevel = allProducts.stream()
                .filter(p -> p.getTrackInventory())
                .mapToInt(p -> p.getInventoryQuantity() != null ? p.getInventoryQuantity() : 0)
                .average()
                .orElse(0.0);

        // Build the report based on type
        return switch (reportType) {
            case SUMMARY -> InventoryReportDto.createSummary(
                totalProducts, totalStockValue, lowStockCount, outOfStockCount, 
                totalStockUnits, averageStockLevel);
            
            case DETAILED -> createDetailedReport(allProducts, totalProducts, totalStockValue, 
                lowStockCount, outOfStockCount, totalStockUnits, averageStockLevel, includeCategoryBreakdown);
            
            case LOW_STOCK -> createLowStockReport(allProducts, totalProducts, totalStockValue, 
                lowStockCount, outOfStockCount, totalStockUnits, averageStockLevel);
            
            case VALUATION -> createValuationReport(allProducts, totalProducts, totalStockValue, 
                lowStockCount, outOfStockCount, totalStockUnits, averageStockLevel);
        };
    }

    @Override
    public boolean hasSufficientStock(UUID storeId, UUID productId, Integer requiredQuantity) {
        Integer currentStock = getCurrentStockLevel(storeId, productId);
        if (currentStock == null) {
            return false;
        }

        // Check if product tracks inventory
        Optional<Product> productOpt = productRepository.findByIdOptional(productId);
        if (productOpt.isEmpty() || !productOpt.get().getTrackInventory()) {
            return true; // If not tracking inventory, assume sufficient
        }

        return currentStock >= requiredQuantity;
    }

    @Override
    public boolean reserveStock(UUID storeId, UUID productId, Integer quantity, String reservationId) {
        Log.infof("Reserving stock: product=%s, quantity=%d, reservation=%s", 
                  productId, quantity, reservationId);

        if (!hasSufficientStock(storeId, productId, quantity)) {
            Log.warnf("Insufficient stock for reservation: product=%s, required=%d", productId, quantity);
            return false;
        }

        StockReservation reservation = new StockReservation(
            reservationId, productId, quantity, Instant.now()
        );

        stockReservations.put(reservationId, reservation);
        Log.infof("Stock reserved successfully: %s", reservationId);
        return true;
    }

    @Override
    public boolean releaseStockReservation(UUID storeId, String reservationId) {
        Log.infof("Releasing stock reservation: %s", reservationId);
        
        StockReservation reservation = stockReservations.remove(reservationId);
        if (reservation != null) {
            Log.infof("Stock reservation released: %s", reservationId);
            return true;
        }
        
        Log.warnf("Reservation not found: %s", reservationId);
        return false;
    }

    @Override
    @Transactional
    public boolean confirmStockReservation(UUID storeId, String reservationId, String reason) {
        Log.infof("Confirming stock reservation: %s", reservationId);
        
        StockReservation reservation = stockReservations.get(reservationId);
        if (reservation == null) {
            Log.warnf("Reservation not found: %s", reservationId);
            return false;
        }

        // Create a decrease adjustment
        InventoryAdjustmentDto adjustmentDto = InventoryAdjustmentDto.decrease(
            reservation.productId, reservation.quantity, reason);

        boolean success = recordInventoryAdjustment(storeId, adjustmentDto);
        if (success) {
            stockReservations.remove(reservationId);
            Log.infof("Stock reservation confirmed and removed: %s", reservationId);
        }

        return success;
    }

    @Override
    public Integer getCurrentStockLevel(UUID storeId, UUID productId) {
        Optional<Product> productOpt = productRepository.findByIdOptional(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            return product.getTrackInventory() ? product.getInventoryQuantity() : null;
        }
        return null;
    }

    @Override
    @Transactional
    public boolean bulkUpdateInventory(UUID storeId, List<InventoryUpdateDto> updates) {
        Log.infof("Performing bulk inventory update for store %s: %d products", storeId, updates.size());

        try {
            for (InventoryUpdateDto update : updates) {
                if (!updateProductInventory(storeId, update.productId(), update)) {
                    Log.errorf("Failed to update product %s during bulk operation", update.productId());
                    throw new RuntimeException("Bulk update failed for product: " + update.productId());
                }
            }
            Log.infof("Bulk inventory update completed successfully for %d products", updates.size());
            return true;
        } catch (Exception e) {
            Log.errorf(e, "Bulk inventory update failed for store %s", storeId);
            return false;
        }
    }

    // Private helper methods

    private boolean validateStockUpdate(Product product, InventoryUpdateDto updateDto) {
        if (!product.getTrackInventory()) {
            Log.warnf("Attempting to update inventory for product that doesn't track inventory: %s", product.getId());
            return false;
        }

        if (updateDto.newQuantity() < 0) {
            Log.warnf("Invalid negative stock quantity: %d", updateDto.newQuantity());
            return false;
        }

        if (updateDto.lowStockThreshold() != null && updateDto.lowStockThreshold() <= 0) {
            Log.warnf("Invalid low stock threshold: %d", updateDto.lowStockThreshold());
            return false;
        }

        return true;
    }

    private Integer calculateNewQuantity(Integer currentQuantity, InventoryAdjustmentDto adjustmentDto) {
        return switch (adjustmentDto.adjustmentType()) {
            case INCREASE -> currentQuantity + adjustmentDto.quantity();
            case DECREASE -> currentQuantity - adjustmentDto.quantity();
            case SET -> adjustmentDto.quantity();
        };
    }

    private InventoryAdjustment.AdjustmentType mapAdjustmentType(InventoryAdjustmentDto.AdjustmentType dtoType) {
        return switch (dtoType) {
            case INCREASE -> InventoryAdjustment.AdjustmentType.INCREASE;
            case DECREASE -> InventoryAdjustment.AdjustmentType.DECREASE;
            case SET -> InventoryAdjustment.AdjustmentType.SET;
        };
    }

    private InventoryAdjustment createAdjustment(UUID productId, InventoryAdjustment.AdjustmentType type,
                                               Integer quantity, Integer previousQuantity, Integer newQuantity,
                                               String reason, String reference, String notes) {
        return InventoryAdjustment.builder()
                .productId(productId)
                .adjustmentType(type)
                .quantity(quantity)
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .reason(reason)
                .reference(reference)
                .notes(notes)
                .adjustedBy(getCurrentUserName())
                .build();
    }

    private String getCurrentUserName() {
        return securityIdentity != null && securityIdentity.getPrincipal() != null 
               ? securityIdentity.getPrincipal().getName() 
               : "system";
    }

    private boolean isLowStock(Product product) {
        if (!product.getTrackInventory() || product.getInventoryQuantity() == null || product.getLowStockThreshold() == null) {
            return false;
        }
        return product.getInventoryQuantity() <= product.getLowStockThreshold();
    }

    private boolean isOutOfStock(Product product) {
        if (!product.getTrackInventory() || product.getInventoryQuantity() == null) {
            return false;
        }
        return product.getInventoryQuantity() <= 0;
    }

    private LowStockAlertDto mapToLowStockAlert(Product product) {
        // Calculate days at current level (simplified - use last adjustment date in real implementation)
        Integer daysAtCurrentLevel = calculateDaysAtCurrentLevel(product);

        return new LowStockAlertDto(
            product.getId(),
            product.getName(),
            product.getSku(),
            product.getInventoryQuantity(),
            product.getLowStockThreshold(),
            product.getCategory() != null ? product.getCategory().getName() : null,
            product.getPrice(),
            product.getUpdatedAt(),
            daysAtCurrentLevel
        );
    }

    private Integer calculateDaysAtCurrentLevel(Product product) {
        // In a real implementation, query the most recent adjustment
        InventoryAdjustment latestAdjustment = adjustmentRepository.findLatestByProductId(product.getId());
        if (latestAdjustment != null) {
            return (int) Duration.between(latestAdjustment.getAdjustedAt(), Instant.now()).toDays();
        }
        return 0;
    }

    private InventoryReportDto createDetailedReport(List<Product> products, Integer totalProducts, 
                                                   BigDecimal totalStockValue, Integer lowStockCount, 
                                                   Integer outOfStockCount, Integer totalStockUnits, 
                                                   Double averageStockLevel, boolean includeCategoryBreakdown) {
        List<LowStockAlertDto> topLowStockItems = products.stream()
                .filter(this::isLowStock)
                .map(this::mapToLowStockAlert)
                .sorted((a, b) -> Integer.compare(a.getStockPercentage(), b.getStockPercentage()))
                .limit(10)
                .collect(Collectors.toList());

        List<InventoryReportDto.CategoryStockDto> categoryBreakdown = null;
        if (includeCategoryBreakdown) {
            categoryBreakdown = createCategoryBreakdown(products);
        }

        return new InventoryReportDto(
            InventoryReportDto.ReportType.DETAILED,
            Instant.now(),
            totalProducts,
            totalStockValue,
            lowStockCount,
            outOfStockCount,
            totalStockUnits,
            averageStockLevel,
            topLowStockItems,
            categoryBreakdown,
            null // Period comparison would require historical data
        );
    }

    private InventoryReportDto createLowStockReport(List<Product> products, Integer totalProducts, 
                                                   BigDecimal totalStockValue, Integer lowStockCount, 
                                                   Integer outOfStockCount, Integer totalStockUnits, 
                                                   Double averageStockLevel) {
        List<LowStockAlertDto> lowStockItems = products.stream()
                .filter(this::isLowStock)
                .map(this::mapToLowStockAlert)
                .sorted((a, b) -> Integer.compare(a.getStockPercentage(), b.getStockPercentage()))
                .collect(Collectors.toList());

        return new InventoryReportDto(
            InventoryReportDto.ReportType.LOW_STOCK,
            Instant.now(),
            totalProducts,
            totalStockValue,
            lowStockCount,
            outOfStockCount,
            totalStockUnits,
            averageStockLevel,
            lowStockItems,
            null,
            null
        );
    }

    private InventoryReportDto createValuationReport(List<Product> products, Integer totalProducts, 
                                                    BigDecimal totalStockValue, Integer lowStockCount, 
                                                    Integer outOfStockCount, Integer totalStockUnits, 
                                                    Double averageStockLevel) {
        return new InventoryReportDto(
            InventoryReportDto.ReportType.VALUATION,
            Instant.now(),
            totalProducts,
            totalStockValue,
            lowStockCount,
            outOfStockCount,
            totalStockUnits,
            averageStockLevel,
            null,
            null,
            null
        );
    }

    private List<InventoryReportDto.CategoryStockDto> createCategoryBreakdown(List<Product> products) {
        return products.stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.groupingBy(p -> p.getCategory().getName()))
                .entrySet()
                .stream()
                .map(entry -> {
                    String categoryName = entry.getKey();
                    List<Product> categoryProducts = entry.getValue();
                    
                    int productCount = categoryProducts.size();
                    int totalUnits = categoryProducts.stream()
                            .filter(p -> p.getTrackInventory())
                            .mapToInt(p -> p.getInventoryQuantity() != null ? p.getInventoryQuantity() : 0)
                            .sum();
                    
                    BigDecimal totalValue = categoryProducts.stream()
                            .filter(p -> p.getTrackInventory() && p.getInventoryQuantity() != null && p.getCostPrice() != null)
                            .map(p -> p.getCostPrice().multiply(BigDecimal.valueOf(p.getInventoryQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    int lowStockProducts = (int) categoryProducts.stream()
                            .filter(this::isLowStock)
                            .count();

                    return new InventoryReportDto.CategoryStockDto(
                        categoryName, productCount, totalUnits, totalValue, lowStockProducts
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Internal class for stock reservations.
     */
    private static class StockReservation {
        final String reservationId;
        final UUID productId;
        final Integer quantity;
        final Instant reservedAt;

        StockReservation(String reservationId, UUID productId, Integer quantity, Instant reservedAt) {
            this.reservationId = reservationId;
            this.productId = productId;
            this.quantity = quantity;
            this.reservedAt = reservedAt;
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
