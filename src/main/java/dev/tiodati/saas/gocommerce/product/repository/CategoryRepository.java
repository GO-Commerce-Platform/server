package dev.tiodati.saas.gocommerce.product.repository;

import dev.tiodati.saas.gocommerce.product.entity.Category;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Category entity operations.
 * Provides database access methods for category management.
 */
@ApplicationScoped
public class CategoryRepository implements PanacheRepositoryBase<Category, UUID> {

    /**
     * Find all active categories with pagination.
     *
     * @param page the page information
     * @return list of active categories
     */
    public List<Category> findActiveCategories(Page page) {
        return find("isActive = true ORDER BY sortOrder, name")
                .page(page)
                .list();
    }

    /**
     * Find active category by slug.
     *
     * @param slug the category slug
     * @return optional category
     */
    public Optional<Category> findBySlug(String slug) {
        return find("slug = ?1 AND isActive = true", slug)
                .firstResultOptional();
    }

    /**
     * Find active subcategories of a parent category.
     *
     * @param parentId the parent category ID
     * @return list of subcategories
     */
    public List<Category> findSubcategories(UUID parentId) {
        return find("parent.id = ?1 AND isActive = true ORDER BY sortOrder, name", parentId)
                .list();
    }

    /**
     * Find all root categories (no parent).
     *
     * @return list of root categories
     */
    public List<Category> findRootCategories() {
        return find("parent IS NULL AND isActive = true ORDER BY sortOrder, name")
                .list();
    }

    /**
     * Find featured categories.
     *
     * @return list of featured categories
     */
    public List<Category> findFeaturedCategories() {
        return find("isFeatured = true AND isActive = true ORDER BY sortOrder, name")
                .list();
    }

    /**
     * Soft delete a category by setting it inactive.
     *
     * @param categoryId the category ID
     * @return number of updated records
     */
    public int softDelete(UUID categoryId) {
        return update("isActive = false WHERE id = ?1", categoryId);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
