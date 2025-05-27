package dev.tiodati.saas.gocommerce.product.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.product.dto.CreateProductDto;
import dev.tiodati.saas.gocommerce.product.dto.ProductDto;

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
}
