package dev.tiodati.saas.gocommerce.product.repository;

import dev.tiodati.saas.gocommerce.product.dto.ProductAvailability;
import dev.tiodati.saas.gocommerce.product.entity.Product;
import dev.tiodati.saas.gocommerce.product.entity.ProductStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
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

    /**
     * Search products by name, description, or SKU.
     *
     * @param searchTerm the search term
     * @param page       the page information
     * @return list of matching products
     */
    public List<Product> searchProducts(String searchTerm, Page page) {
        String likePattern = "%" + searchTerm.toLowerCase() + "%";
        return find(
                "(LOWER(name) LIKE ?1 OR LOWER(description) LIKE ?1 OR LOWER(sku) LIKE ?1) AND status = ?2 ORDER BY name",
                likePattern, ProductStatus.ACTIVE)
                .page(page)
                .list();
    }

    /**
     * Search products with advanced filters.
     *
     * @param searchTerm the search term (optional)
     * @param categoryId the category ID (optional)
     * @param minPrice   minimum price (optional)
     * @param maxPrice   maximum price (optional)
     * @param inStock    only products in stock (optional)
     * @param page       the page information
     * @return list of matching products
     */
    public List<Product> searchProductsAdvanced(String searchTerm, UUID categoryId,
                                                BigDecimal minPrice, BigDecimal maxPrice,
                                                Boolean inStock, Page page) {
        StringBuilder query = new StringBuilder("status = ?1");
        java.util.List<Object> paramList = new java.util.ArrayList<>();
        paramList.add(ProductStatus.ACTIVE);

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String likePattern = "%" + searchTerm.toLowerCase() + "%";
            query.append(" AND (LOWER(name) LIKE ?" + (paramList.size() + 1));
            query.append(" OR LOWER(description) LIKE ?" + (paramList.size() + 2));
            query.append(" OR LOWER(sku) LIKE ?" + (paramList.size() + 3) + ")");
            paramList.add(likePattern);
            paramList.add(likePattern);
            paramList.add(likePattern);
        }

        if (categoryId != null) {
            query.append(" AND category.id = ?" + (paramList.size() + 1));
            paramList.add(categoryId);
        }

        if (minPrice != null) {
            query.append(" AND price >= ?" + (paramList.size() + 1));
            paramList.add(minPrice);
        }

        if (maxPrice != null) {
            query.append(" AND price <= ?" + (paramList.size() + 1));
            paramList.add(maxPrice);
        }

        if (inStock != null && inStock) {
            query.append(" AND (trackInventory = false OR inventoryQuantity > 0)");
        }

        query.append(" ORDER BY name");

        return find(query.toString(), paramList.toArray())
                .page(page)
                .list();
    }

    /**
     * Find featured products.
     *
     * @param page the page information
     * @return list of featured products
     */
    public List<Product> findFeaturedProducts(Page page) {
        return find("isFeatured = true AND status = ?1 ORDER BY name", ProductStatus.ACTIVE)
                .page(page)
                .list();
    }

    /**
     * Find products that are in stock.
     *
     * @param page the page information
     * @return list of products in stock
     */
    public List<Product> findInStockProducts(Page page) {
        return find("(trackInventory = false OR inventoryQuantity > 0) AND status = ?1 ORDER BY name",
                ProductStatus.ACTIVE)
                .page(page)
                .list();
    }
    
    /**
     * Check product availability for a specific product.
     * Returns detailed availability information including stock status.
     *
     * @param productId the product ID
     * @return product availability information, or null if product not found
     */
    public ProductAvailability checkProductAvailability(UUID productId) {
        Optional<Product> productOpt = findByIdOptional(productId);
        
        if (productOpt.isEmpty()) {
            return null;
        }
        
        Product product = productOpt.get();
        boolean isActive = ProductStatus.ACTIVE.equals(product.getStatus());
        boolean hasStock = !product.getTrackInventory() || product.getInventoryQuantity() > 0;
        boolean isLowStock = product.getTrackInventory() && 
                product.getInventoryQuantity() <= product.getLowStockThreshold() && 
                product.getInventoryQuantity() > 0;
        boolean isOutOfStock = product.getTrackInventory() && product.getInventoryQuantity() <= 0;
        
        String message;
        if (!isActive) {
            message = "Product is not currently available";
        } else if (isOutOfStock) {
            message = "Out of stock";
        } else if (isLowStock) {
            message = "Low stock: only " + product.getInventoryQuantity() + " remaining";
        } else if (product.getTrackInventory()) {
            message = "In stock: " + product.getInventoryQuantity() + " available";
        } else {
            message = "In stock";
        }
        
        return ProductAvailability.builder()
                .productId(product.getId())
                .productName(product.getName())
                .available(isActive && hasStock)
                .status(product.getStatus())
                .trackInventory(product.getTrackInventory())
                .stockQuantity(product.getInventoryQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .lowStock(isLowStock)
                .outOfStock(isOutOfStock)
                .availabilityMessage(message)
                .build();
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
