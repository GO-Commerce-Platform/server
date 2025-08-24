package dev.tiodati.saas.gocommerce.product.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.product.dto.CreateProductDto;
import dev.tiodati.saas.gocommerce.product.dto.ProductDto;
import dev.tiodati.saas.gocommerce.product.dto.ProductAvailabilityDto;

/**
 * Service interface for product operations
 */
public interface ProductService {

    /**
     * List products for a store with pagination and optional category filter
     *
     * @param storeId The store ID
     * @param page Page number (0-based)
     * @param size Page size
     * @param categoryId Optional category filter
     * @return List of product DTOs
     */
    List<ProductDto> listProducts(UUID storeId, int page, int size, UUID categoryId);

    /**
     * Find a product by ID
     *
     * @param storeId The store ID
     * @param productId The product ID
     * @return Optional containing the product if found
     */
    Optional<ProductDto> findProduct(UUID storeId, UUID productId);

    /**
     * Create a new produc
     *
     * @param storeId The store ID
     * @param productDto Product data
     * @return The created produc
     */
    ProductDto createProduct(UUID storeId, CreateProductDto productDto);

    /**
     * Update an existing produc
     *
     * @param storeId The store ID
     * @param productDto Updated product data
     * @return Optional containing the updated product if found
     */
    Optional<ProductDto> updateProduct(UUID storeId, ProductDto productDto);

    /**
     * Delete a product (soft delete)
     *
     * @param storeId The store ID
     * @param productId The product ID
     * @return true if product was deleted, false if not found
     */
    boolean deleteProduct(UUID storeId, UUID productId);

    /**
     * Update inventory levels for multiple products
     *
     * @param storeId The store ID
     * @param inventoryUpdates Map of product IDs to new stock quantities
     */
    void updateInventory(UUID storeId, Map<UUID, Integer> inventoryUpdates);

    /**
     * Search products by text query with optional filters
     *
     * @param storeId The store ID
     * @param query Search query text
     * @param categoryId Optional category filter
     * @param minPrice Optional minimum price filter
     * @param maxPrice Optional maximum price filter
     * @param inStock Optional filter for products in stock
     * @param page Page number (0-based)
     * @param size Page size
     * @return List of matching products
     */
    List<ProductDto> searchProducts(UUID storeId, String query, UUID categoryId, 
                                   Double minPrice, Double maxPrice, Boolean inStock, 
                                   int page, int size);

    /**
     * Get featured products for a store
     *
     * @param storeId The store ID
     * @param page Page number (0-based)
     * @param size Page size
     * @return List of featured products
     */
    List<ProductDto> getFeaturedProducts(UUID storeId, int page, int size);

    /**
     * Get products by category
     *
     * @param storeId The store ID
     * @param categoryId The category ID
     * @param page Page number (0-based)
     * @param size Page size
     * @return List of products in the category
     */
    List<ProductDto> getProductsByCategory(UUID storeId, UUID categoryId, int page, int size);

    /**
     * Check product availability
     *
     * @param storeId The store ID
     * @param productId The product ID
     * @return Availability information
     */
    ProductAvailabilityDto checkProductAvailability(UUID storeId, UUID productId);
}
