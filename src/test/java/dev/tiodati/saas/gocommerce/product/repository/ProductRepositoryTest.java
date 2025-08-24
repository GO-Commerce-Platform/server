package dev.tiodati.saas.gocommerce.product.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.tiodati.saas.gocommerce.product.dto.ProductAvailability;
import dev.tiodati.saas.gocommerce.product.entity.Category;
import dev.tiodati.saas.gocommerce.product.entity.Product;
import dev.tiodati.saas.gocommerce.product.entity.ProductStatus;
import dev.tiodati.saas.gocommerce.testinfra.MultiTenantTestExtension;
import io.quarkus.panache.common.Page;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import io.quarkus.logging.Log;

/**
 * Unit tests for ProductRepository's new search and availability methods.
 * Tests cover search functionality, featured products, and availability checks.
 */
@QuarkusTest
@ExtendWith(MultiTenantTestExtension.class)
@TestProfile(ProductRepositoryTest.TestProfileImpl.class)
class ProductRepositoryTest {

    @Inject
    ProductRepository productRepository;

    @Inject
    EntityManager entityManager;

    private Category testCategory;
    private Product activeProduct;
    private Product inactiveProduct;
    private Product outOfStockProduct;
    private Product lowStockProduct;
    private Product featuredProduct;

    /**
     * Sets up test data within the current transaction context.
     * Must be called at the beginning of each test method.
     */
    private void setupTestData() {
        // Clear existing data
        productRepository.deleteAll();
        
        // Create test category
        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setSlug("test-category");
        testCategory.setDescription("Test category for repository tests");
        entityManager.persist(testCategory);
        entityManager.flush(); // Ensure category is saved first

        // Create test products
        activeProduct = createTestProduct("Active Product", "ACT-001", ProductStatus.ACTIVE, 10, false);
        inactiveProduct = createTestProduct("Inactive Product", "INA-001", ProductStatus.ARCHIVED, 5, false);
        outOfStockProduct = createTestProduct("Out of Stock", "OOS-001", ProductStatus.ACTIVE, 0, false);
        lowStockProduct = createTestProduct("Low Stock", "LOW-001", ProductStatus.ACTIVE, 2, false);
        featuredProduct = createTestProduct("Featured Product", "FEA-001", ProductStatus.ACTIVE, 15, true);

        // Set low stock threshold
        lowStockProduct.setLowStockThreshold(5);

        // Persist products using persistAndFlush to ensure they're immediately available
        productRepository.persistAndFlush(activeProduct);
        productRepository.persistAndFlush(inactiveProduct);
        productRepository.persistAndFlush(outOfStockProduct);
        productRepository.persistAndFlush(lowStockProduct);
        productRepository.persistAndFlush(featuredProduct);
        
        // Validate that products were persisted correctly
        validateTestDataSetup();
    }

    @Test
    @TestTransaction
    void testSearchProducts_ByName() {
        setupTestData();
        
        Page page = Page.of(0, 10);
        List<Product> results = productRepository.searchProducts("Active", page);

        assertEquals(1, results.size());
        assertEquals("Active Product", results.get(0).getName());
    }

    @Test
    @TestTransaction
    void testSearchProducts_BySku() {
        setupTestData();
        
        Page page = Page.of(0, 10);
        List<Product> results = productRepository.searchProducts("ACT-001", page);

        assertEquals(1, results.size());
        assertEquals("ACT-001", results.get(0).getSku());
    }

    @Test
    @TestTransaction
    void testSearchProducts_NoResults() {
        setupTestData();
        
        Page page = Page.of(0, 10);
        List<Product> results = productRepository.searchProducts("NonExistent", page);

        assertTrue(results.isEmpty());
    }

    @Test
    @TestTransaction
    void testSearchProductsAdvanced_WithAllFilters() {
        setupTestData();
        
        Page page = Page.of(0, 10);
        List<Product> results = productRepository.searchProductsAdvanced(
                "Product",
                testCategory.getId(),
                BigDecimal.valueOf(10.00),
                BigDecimal.valueOf(30.00),
                true,
                page
        );

        // Should only return active products with stock in the price range
        assertTrue(results.size() > 0);
        results.forEach(product -> {
            assertEquals(ProductStatus.ACTIVE, product.getStatus());
            assertTrue(product.getPrice().compareTo(BigDecimal.valueOf(10.00)) >= 0);
            assertTrue(product.getPrice().compareTo(BigDecimal.valueOf(30.00)) <= 0);
        });
    }

    @Test
    @TestTransaction
    void testSearchProductsAdvanced_InStockFilter() {
        setupTestData();
        
        Page page = Page.of(0, 10);
        List<Product> results = productRepository.searchProductsAdvanced(
                null, null, null, null, true, page
        );

        // Should exclude out of stock products
        results.forEach(product -> {
            if (product.getTrackInventory()) {
                assertTrue(product.getInventoryQuantity() > 0);
            }
        });
    }

    @Test
    @TestTransaction
    void testFindFeaturedProducts() {
        setupTestData();
        
        Page page = Page.of(0, 10);
        List<Product> results = productRepository.findFeaturedProducts(page);

        assertEquals(1, results.size());
        assertEquals("Featured Product", results.get(0).getName());
        assertTrue(results.get(0).getIsFeatured());
        assertEquals(ProductStatus.ACTIVE, results.get(0).getStatus());
    }

    @Test
    @TestTransaction
    void testFindInStockProducts() {
        setupTestData();
        
        Page page = Page.of(0, 10);
        List<Product> results = productRepository.findInStockProducts(page);

        // Should exclude out of stock products
        results.forEach(product -> {
            if (product.getTrackInventory()) {
                assertTrue(product.getInventoryQuantity() > 0, 
                    "Product " + product.getName() + " should be in stock");
            }
        });
        
        // Should not include the out of stock product
        assertFalse(results.contains(outOfStockProduct));
    }

    @Test
    @TestTransaction
    void testCheckProductAvailability_ActiveProduct() {
        setupTestData();
        
        ProductAvailability availability = productRepository.checkProductAvailability(activeProduct.getId());

        assertNotNull(availability);
        assertEquals(activeProduct.getId(), availability.getProductId());
        assertEquals("Active Product", availability.getProductName());
        assertTrue(availability.isAvailable());
        assertEquals(ProductStatus.ACTIVE, availability.getStatus());
        assertEquals(10, availability.getStockQuantity());
        assertFalse(availability.isLowStock());
        assertFalse(availability.isOutOfStock());
        assertTrue(availability.getAvailabilityMessage().contains("In stock"));
    }

    @Test
    @TestTransaction
    void testCheckProductAvailability_OutOfStockProduct() {
        setupTestData();
        
        ProductAvailability availability = productRepository.checkProductAvailability(outOfStockProduct.getId());

        assertNotNull(availability);
        assertEquals(outOfStockProduct.getId(), availability.getProductId());
        assertFalse(availability.isAvailable());
        assertTrue(availability.isOutOfStock());
        assertEquals(0, availability.getStockQuantity());
        assertEquals("Out of stock", availability.getAvailabilityMessage());
    }

    @Test
    @TestTransaction
    void testCheckProductAvailability_LowStockProduct() {
        setupTestData();
        
        ProductAvailability availability = productRepository.checkProductAvailability(lowStockProduct.getId());

        assertNotNull(availability);
        assertEquals(lowStockProduct.getId(), availability.getProductId());
        assertTrue(availability.isAvailable()); // Still available but low
        assertTrue(availability.isLowStock());
        assertFalse(availability.isOutOfStock());
        assertEquals(2, availability.getStockQuantity());
        assertTrue(availability.getAvailabilityMessage().contains("Low stock"));
    }

    @Test
    @TestTransaction
    void testCheckProductAvailability_InactiveProduct() {
        setupTestData();
        
        ProductAvailability availability = productRepository.checkProductAvailability(inactiveProduct.getId());

        assertNotNull(availability);
        assertEquals(inactiveProduct.getId(), availability.getProductId());
        assertFalse(availability.isAvailable());
        assertEquals(ProductStatus.ARCHIVED, availability.getStatus());
        assertEquals("Product is not currently available", availability.getAvailabilityMessage());
    }

    @Test
    @TestTransaction
    void testCheckProductAvailability_NonExistentProduct() {
        setupTestData();
        
        UUID nonExistentId = UUID.randomUUID();
        ProductAvailability availability = productRepository.checkProductAvailability(nonExistentId);

        assertNull(availability);
    }

    @Test
    @TestTransaction
    void testSearchProductsAdvanced_PaginationWorks() {
        setupTestData();
        
        // Test first page
        Page firstPage = Page.of(0, 2);
        List<Product> firstPageResults = productRepository.searchProductsAdvanced(
                "Product", null, null, null, null, firstPage
        );

        // Test second page
        Page secondPage = Page.of(1, 2);
        List<Product> secondPageResults = productRepository.searchProductsAdvanced(
                "Product", null, null, null, null, secondPage
        );

        // Verify pagination works (no overlapping results)
        if (!firstPageResults.isEmpty() && !secondPageResults.isEmpty()) {
            assertNotEquals(firstPageResults.get(0).getId(), secondPageResults.get(0).getId());
        }
    }

    /**
     * Validates that test data was correctly created and persisted.
     */
    private void validateTestDataSetup() {
        Log.infof("🔍 Validating test data setup...");
        
        List<Product> allProducts = productRepository.findAll().list();
        Log.infof("📊 Found %d products in database after setup", allProducts.size());
        
        // Assert that we have the expected number of products
        assertEquals(5, allProducts.size(), "Should have 5 test products");
        
        // Log details of each product for debugging
        allProducts.forEach(product -> {
            Log.infof("✅ Product: %s (ID: %s, SKU: %s, Status: %s, Inventory: %d, Featured: %s)", 
                     product.getName(), product.getId(), product.getSku(), 
                     product.getStatus(), product.getInventoryQuantity(), product.getIsFeatured());
        });
        
        Log.infof("✅ Test data setup validation completed successfully");
    }
    
    private Product createTestProduct(String name, String sku, ProductStatus status, 
                                    int inventoryQuantity, boolean isFeatured) {
        Product product = new Product();
        // Don't set ID manually - let Hibernate generate it
        product.setName(name);
        product.setSku(sku);
        product.setDescription("Test description for " + name);
        product.setPrice(BigDecimal.valueOf(19.99));
        product.setCostPrice(BigDecimal.valueOf(10.00));
        product.setStatus(status);
        product.setInventoryQuantity(inventoryQuantity);
        product.setTrackInventory(true);
        product.setLowStockThreshold(3);
        product.setIsFeatured(isFeatured);
        product.setCategory(testCategory);
        return product;
    }

    /**
     * Test profile for repository tests
     */
    public static class TestProfileImpl implements io.quarkus.test.junit.QuarkusTestProfile {
        // Use default test configuration
    }
}
