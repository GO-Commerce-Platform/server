package dev.tiodati.saas.gocommerce.product.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for Product entity and ProductStatus enum.
 */
class ProductEntityTest {

    @Test
    void testProductStatusEnumValues() {
        // Verify all expected enum values exist
        assertEquals(3, ProductStatus.values().length);
        assertEquals(ProductStatus.DRAFT, ProductStatus.valueOf("DRAFT"));
        assertEquals(ProductStatus.ACTIVE, ProductStatus.valueOf("ACTIVE"));
        assertEquals(ProductStatus.ARCHIVED, ProductStatus.valueOf("ARCHIVED"));
    }

    @Test
    void testProductCreation() {
        // Given/When
        var product = Product.builder()
                .id(UUID.randomUUID())
                .sku("TEST-SKU-001")
                .name("Test Product")
                .description("A test product")
                .price(BigDecimal.valueOf(29.99))
                .costPrice(BigDecimal.valueOf(15.50))
                .inventoryQuantity(100)
                .status(ProductStatus.ACTIVE)
                .trackInventory(true)
                .requiresShipping(true)
                .isFeatured(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Then
        assertNotNull(product);
        assertEquals("TEST-SKU-001", product.getSku());
        assertEquals("Test Product", product.getName());
        assertEquals("A test product", product.getDescription());
        assertEquals(BigDecimal.valueOf(29.99), product.getPrice());
        assertEquals(BigDecimal.valueOf(15.50), product.getCostPrice());
        assertEquals(100, product.getInventoryQuantity());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
        assertEquals(true, product.getTrackInventory());
        assertEquals(true, product.getRequiresShipping());
        assertEquals(false, product.getIsFeatured());
    }

    @Test
    void testProductDefaultValues() {
        // When
        var product = Product.builder()
                .name("Default Test Product")
                .price(BigDecimal.valueOf(10.00))
                .build();

        // Then
        assertEquals(ProductStatus.DRAFT, product.getStatus());
        assertEquals(false, product.getIsFeatured());
        assertEquals(true, product.getRequiresShipping());
        assertEquals(true, product.getTrackInventory());
        assertEquals(0, product.getInventoryQuantity());
    }

    @Test
    void testProductWithCategory() {
        // Given
        var category = Category.builder()
                .id(UUID.randomUUID())
                .name("Test Category")
                .build();

        // When
        var product = Product.builder()
                .name("Categorized Product")
                .price(BigDecimal.valueOf(15.00))
                .category(category)
                .build();

        // Then
        assertNotNull(product.getCategory());
        assertEquals("Test Category", product.getCategory().getName());
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
