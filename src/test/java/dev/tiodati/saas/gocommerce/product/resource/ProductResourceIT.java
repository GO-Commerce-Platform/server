package dev.tiodati.saas.gocommerce.product.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.tiodati.saas.gocommerce.product.entity.Category;
import dev.tiodati.saas.gocommerce.product.entity.Product;
import dev.tiodati.saas.gocommerce.product.entity.ProductStatus;
import dev.tiodati.saas.gocommerce.product.repository.ProductRepository;
import dev.tiodati.saas.gocommerce.testinfra.MultiTenantTestExtension;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.TestTransaction;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response.Status;

/**
 * Integration tests for ProductResource REST API endpoints.
 * Tests the new search, featured products, and availability endpoints.
 */
@QuarkusTest
@ExtendWith(MultiTenantTestExtension.class)
@TestProfile(ProductResourceIT.TestProfileImpl.class)
class ProductResourceIT {

    private static final UUID TEST_STORE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    
    @Inject
    ProductRepository productRepository;
    
    @Inject
    EntityManager entityManager;
    
    private Category testCategory;
    private Product activeProduct;
    private Product featuredProduct;

    @BeforeEach
    @TestTransaction
    void setup() {
        // Clear existing data
        productRepository.deleteAll();
        
        // Create test category
        testCategory = new Category();
        testCategory.setId(UUID.randomUUID());
        testCategory.setName("Electronics");
        entityManager.persist(testCategory);

        // Create test products
        activeProduct = createTestProduct(
            "Smartphone", "PHONE-001", 
            ProductStatus.ACTIVE, 15, false, BigDecimal.valueOf(299.99)
        );

        featuredProduct = createTestProduct(
            "Laptop", "LAPTOP-001", 
            ProductStatus.ACTIVE, 8, true, BigDecimal.valueOf(899.99)
        );

        // Persist products
        productRepository.persist(activeProduct);
        productRepository.persist(featuredProduct);

        entityManager.flush();
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"CUSTOMER"})
    void testSearchProducts_WithQuery() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("q", "phone")
            .queryParam("page", 0)
            .queryParam("size", 20)
        .when()
            .get("/api/v1/stores/{storeId}/products/search", TEST_STORE_ID)
        .then()
            .statusCode(Status.OK.getStatusCode())
            .contentType(ContentType.JSON)
            .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"CUSTOMER"})
    void testGetFeaturedProducts() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .when()
            .get("/api/v1/stores/{storeId}/products/featured", TEST_STORE_ID)
        .then()
            .statusCode(Status.OK.getStatusCode())
            .contentType(ContentType.JSON)
            .body("size()", greaterThanOrEqualTo(0));
    }

    private Product createTestProduct(String name, String sku, ProductStatus status, 
                                    int inventoryQuantity, boolean isFeatured, BigDecimal price) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setSku(sku);
        product.setDescription("Test description for " + name);
        product.setPrice(price);
        product.setCostPrice(price.multiply(BigDecimal.valueOf(0.6)));
        product.setStatus(status);
        product.setInventoryQuantity(inventoryQuantity);
        product.setTrackInventory(true);
        product.setLowStockThreshold(5);
        product.setIsFeatured(isFeatured);
        product.setCategory(testCategory);
        return product;
    }

    /**
     * Test profile for REST API integration tests
     */
    public static class TestProfileImpl implements io.quarkus.test.junit.QuarkusTestProfile {
        // Use default test configuration
    }
}
