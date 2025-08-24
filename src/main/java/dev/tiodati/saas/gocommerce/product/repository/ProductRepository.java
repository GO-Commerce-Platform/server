package dev.tiodati.saas.gocommerce.product.repository;

import dev.tiodati.saas.gocommerce.product.entity.Product;
import dev.tiodati.saas.gocommerce.product.entity.ProductStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Product entity operations.
 * Provides database access methods for product management.
 */
@ApplicationScoped
public class ProductRepository implements PanacheRepositoryBase<Product, UUID> {

    /**
     * Find all active products with pagination.
     *
     * @param page the page information
     * @return list of active products
     */
    public List<Product> findActiveProducts(Page page) {
        return find("status = ?1 ORDER BY name", ProductStatus.ACTIVE)
                .page(page)
                .list();
    }

    /**
     * Find products by category with pagination.
     *
     * @param categoryId the category ID
     * @param page       the page information
     * @return list of products in the category
     */
    public List<Product> findByCategory(UUID categoryId, Page page) {
        return find("category.id = ?1 AND status = ?2 ORDER BY name",
                categoryId, ProductStatus.ACTIVE)
                .page(page)
                .list();
    }

    /**
     * Find active product by SKU.
     *
     * @param sku the product SKU
     * @return optional product
     */
    public Optional<Product> findBySku(String sku) {
        return find("sku = ?1 AND status = ?2", sku, ProductStatus.ACTIVE)
                .firstResultOptional();
    }

    /**
     * Find active product by slug.
     *
     * @param slug the product slug
     * @return optional product
     */
    public Optional<Product> findBySlug(String slug) {
        return find("slug = ?1 AND status = ?2", slug, ProductStatus.ACTIVE)
                .firstResultOptional();
    }

    /**
     * Find products with low stock (below minimum threshold).
     *
     * @param threshold the minimum stock threshold
     * @return list of products with low stock
     */
    public List<Product> findLowStock(int threshold) {
        return find("inventoryQuantity <= ?1 AND status = ?2", threshold, ProductStatus.ACTIVE)
                .list();
    }

    /**
     * Update stock quantity for a product.
     *
     * @param productId   the product ID
     * @param newQuantity the new stock quantity
     * @return number of updated records
     */
    public int updateStock(UUID productId, int newQuantity) {
        return update("inventoryQuantity = ?1 WHERE id = ?2", newQuantity, productId);
    }

    /**
     * Soft delete a product by changing its status to ARCHIVED.
     *
     * @param productId the product ID
     * @return number of updated records
     */
    public int softDelete(UUID productId) {
        return update("status = ?1 WHERE id = ?2", ProductStatus.ARCHIVED, productId);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
