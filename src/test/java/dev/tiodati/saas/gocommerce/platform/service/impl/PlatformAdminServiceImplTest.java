package dev.tiodati.saas.gocommerce.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.platform.api.dto.AdminUserDetails;
import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.api.dto.StoreResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class PlatformAdminServiceImplTest {

    @Inject
    PlatformAdminServiceImpl platformAdminService;
    
    // If using mocks, uncomment these and modify the test
    /*
    @Mock
    PlatformStoreRepository storeRepository;
    
    @InjectMocks
    PlatformAdminServiceImpl platformAdminService;
    */

    private CreateStoreRequest createStoreRequest;

    @BeforeEach
    void setUp() {
        AdminUserDetails adminUser = new AdminUserDetails(
            "Jane",
            "Doe",
            "jane.doe@example.com",
            "securepass123"
        );

        createStoreRequest = new CreateStoreRequest(
            "Test Store Service",
            "test-store-service",
            adminUser
        );
        
        // If using mocks, uncomment this
        // when(storeRepository.existsBySubdomain(any())).thenReturn(false);
    }

    @Test
    void testCreateStore() {
        // Clean up any existing test data with the same subdomain before running the test
        // (This helps avoid conflicts if tests are run multiple times)
        try {
            StoreResponse response = platformAdminService.createStore(createStoreRequest);
            
            // Verify the store was created with the correct data
            assertNotNull(response, "Response should not be null");
            assertNotNull(response.id(), "Store ID should not be null");
            assertEquals("Test Store Service", response.storeName(), "Store name should match request");
            assertEquals("test-store-service", response.subdomain(), "Store subdomain should match request");
            assertEquals("test-store-service.gocommerce.com", response.fullDomain(), "Store full domain should be correctly formed");
            assertEquals("ACTIVE", response.status(), "Store status should be ACTIVE");
            
            // This was failing - now explicitly assert on createdAt
            assertNotNull(response.createdAt(), "Created timestamp should not be null");
        } catch (Exception e) {
            // Log the error for debugging purposes
            System.err.println("Error in test: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
