package dev.tiodati.saas.gocommerce.product.service;

import dev.tiodati.saas.gocommerce.product.dto.CreateProductKitDto;
import dev.tiodati.saas.gocommerce.product.dto.ProductKitDto;
import dev.tiodati.saas.gocommerce.testinfra.MultiTenantTestExtension;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@ExtendWith(MultiTenantTestExtension.class)
class ProductKitServiceTest {

    @Inject
    ProductKitService productKitService;

    @Test
    @TestTransaction
    void testCreateProductKit() {
        UUID storeId = UUID.randomUUID();
        CreateProductKitDto createDto = new CreateProductKitDto(
                "Test Kit",
                "Test Description",
                BigDecimal.TEN,
                true,
                Collections.emptyList()
        );

        ProductKitDto createdKit = productKitService.create(storeId, createDto);

        assertNotNull(createdKit.id());
        assertEquals("Test Kit", createdKit.name());
        assertEquals(storeId, createdKit.storeId());
    }
}
