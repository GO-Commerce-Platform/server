package dev.tiodati.saas.gocommerce.product.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import dev.tiodati.saas.gocommerce.product.dto.CreateProductDto;
import dev.tiodati.saas.gocommerce.product.dto.ProductAvailability;
import dev.tiodati.saas.gocommerce.product.dto.ProductAvailabilityDto;
import dev.tiodati.saas.gocommerce.product.dto.ProductDto;
import dev.tiodati.saas.gocommerce.product.entity.Product;
import dev.tiodati.saas.gocommerce.product.entity.ProductStatus;
import dev.tiodati.saas.gocommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of ProductService providing store-specific product management
 * functionality.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    /**
     * Repository for product data access operations.
     */
    private final ProductRepository productRepository;

    @Override
    public List<ProductDto> listProducts(UUID storeId, int page, int size, UUID categoryId) {
        Log.infof("Listing products for store %s (page=%d, size=%d, categoryId=%s)",
                storeId, page, size, categoryId);

        List<Product> products;
        var pageable = Page.of(page, size);

        if (categoryId != null) {
            products = productRepository.findByCategory(categoryId, pageable);
        } else {
            products = productRepository.findActiveProducts(pageable);
        }

        return products.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public Optional<ProductDto> findProduct(UUID storeId, UUID productId) {
        Log.infof("Finding product %s for store %s", productId, storeId);

        return productRepository.findByIdOptional(productId)
                .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public ProductDto createProduct(UUID storeId, CreateProductDto productDto) {
        Log.infof("Creating product for store %s: %s", storeId, productDto.name());

        var product = new Product();
        product.setSku(productDto.sku());
        product.setName(productDto.name());
        product.setDescription(productDto.description());
        product.setPrice(productDto.price());
        product.setCostPrice(productDto.cost());
        product.setInventoryQuantity(productDto.inventoryQuantity());
        product.setStatus(ProductStatus.ACTIVE);

        // Map category if provided
        if (productDto.categoryId() != null) {
            var category = new dev.tiodati.saas.gocommerce.product.entity.Category();
            category.setId(productDto.categoryId());
            product.setCategory(category);
        }

        var now = Instant.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        productRepository.persist(product);

        return mapToDto(product);
    }

    @Override
    @Transactional
    public Optional<ProductDto> updateProduct(UUID storeId, ProductDto productDto) {
        Log.infof("Updating product %s for store %s", productDto.id(), storeId);

        return productRepository.findByIdOptional(productDto.id())
                .map(product -> {
                    product.setSku(productDto.sku());
                    product.setName(productDto.name());
                    product.setDescription(productDto.description());
                    product.setPrice(productDto.price());
                    product.setCostPrice(productDto.cost());
                    product.setInventoryQuantity(productDto.inventoryQuantity());
                    product.setStatus(productDto.isActive() ? ProductStatus.ACTIVE : ProductStatus.ARCHIVED);

                    // Update category if provided
                    if (productDto.categoryId() != null) {
                        var category = new dev.tiodati.saas.gocommerce.product.entity.Category();
                        category.setId(productDto.categoryId());
                        product.setCategory(category);
                    }

                    product.setUpdatedAt(Instant.now());

                    productRepository.persist(product);
                    return mapToDto(product);
                });
    }

    @Override
    @Transactional
    public boolean deleteProduct(UUID storeId, UUID productId) {
        Log.infof("Deleting product %s for store %s", productId, storeId);

        return productRepository.findByIdOptional(productId)
                .map(product -> {
                    productRepository.softDelete(productId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public void updateInventory(UUID storeId, Map<UUID, Integer> inventoryUpdates) {
        Log.infof("Updating inventory for store %s: %d products", storeId, inventoryUpdates.size());

        inventoryUpdates.forEach((productId, quantity) -> {
            productRepository.findByIdOptional(productId)
                    .ifPresentOrElse(
                            product -> productRepository.updateStock(productId, quantity),
                            () -> Log.warnf("Product %s not found", productId));
        });
    }

    @Override
    public List<ProductDto> searchProducts(UUID storeId, String query, UUID categoryId,
                                          Double minPrice, Double maxPrice, Boolean inStock,
                                          int page, int size) {
        Log.infof("Searching products for store %s: query='%s', category=%s, price=%s-%s, inStock=%s",
                storeId, query, categoryId, minPrice, maxPrice, inStock);

        var pageable = Page.of(page, size);
        
        if (query == null || query.trim().isEmpty()) {
            // No search term - use advanced filtering
            BigDecimal minPriceBD = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
            BigDecimal maxPriceBD = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;
            
            List<Product> products = productRepository.searchProductsAdvanced(
                    null, categoryId, minPriceBD, maxPriceBD, inStock, pageable);
            
            return products.stream()
                    .map(this::mapToDto)
                    .toList();
        } else {
            // Text search with filters
            BigDecimal minPriceBD = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
            BigDecimal maxPriceBD = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;
            
            List<Product> products = productRepository.searchProductsAdvanced(
                    query.trim(), categoryId, minPriceBD, maxPriceBD, inStock, pageable);
            
            return products.stream()
                    .map(this::mapToDto)
                    .toList();
        }
    }

    @Override
    public List<ProductDto> getFeaturedProducts(UUID storeId, int page, int size) {
        Log.infof("Getting featured products for store %s (page=%d, size=%d)", storeId, page, size);

        var pageable = Page.of(page, size);
        List<Product> products = productRepository.findFeaturedProducts(pageable);

        return products.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public List<ProductDto> getProductsByCategory(UUID storeId, UUID categoryId, int page, int size) {
        Log.infof("Getting products by category %s for store %s (page=%d, size=%d)",
                categoryId, storeId, page, size);

        var pageable = Page.of(page, size);
        List<Product> products = productRepository.findByCategory(categoryId, pageable);

        return products.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public ProductAvailabilityDto checkProductAvailability(UUID storeId, UUID productId) {
        Log.infof("Checking availability for product %s in store %s", productId, storeId);

        ProductAvailability availability = productRepository.checkProductAvailability(productId);
        
        if (availability == null) {
            return ProductAvailabilityDto.inactive(productId, "Product not found");
        }
        
        return mapToAvailabilityDto(availability);
    }

    /**
     * Maps a Product entity to ProductDto.
     *
     * @param product the Product entity to map
     * @return the corresponding ProductDto
     */
    private ProductDto mapToDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCostPrice(),
                product.getInventoryQuantity(),
                product.getStatus() == ProductStatus.ACTIVE,
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    /**
     * Maps a ProductAvailability to ProductAvailabilityDto.
     *
     * @param availability the ProductAvailability to map
     * @return the corresponding ProductAvailabilityDto
     */
    private ProductAvailabilityDto mapToAvailabilityDto(ProductAvailability availability) {
        return new ProductAvailabilityDto(
                availability.getProductId(),
                availability.isAvailable(),
                !availability.isOutOfStock(),
                availability.getStockQuantity(),
                availability.isLowStock(),
                availability.getAvailabilityMessage()
        );
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
