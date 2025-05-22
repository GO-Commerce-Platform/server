package dev.tiodati.saas.gocommerce.platform.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.platform.api.dto.AdminUserDetails;
import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.entity.PlatformStores;
import dev.tiodati.saas.gocommerce.platform.entity.StoreStatus;
import dev.tiodati.saas.gocommerce.platform.repository.PlatformStoreRepository;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

@QuarkusIntegrationTest
public class PlatformAdminResourceIT {

    @Inject
    PlatformStoreRepository storeRepository;

    @Test
    @TestSecurity(user = "admin", roles = {"Platform Admin"})
    void testCreateStoreEndpoint_PersistsDataCorrectly() {
        // 1. ARRANGE: Set up request data
        AdminUserDetails adminUser = new AdminUserDetails(
            "Integration", 
            "Test", 
            "integration.test@example.com", 
            "test1234"
        );
        
        String storeName = "Integration Test Store";
        String subdomain = "integration-test-" + System.currentTimeMillis(); // Ensure unique subdomain
        
        CreateStoreRequest request = new CreateStoreRequest(
            storeName, 
            subdomain,
            adminUser
        );

        // 2. ACT: Call the API
        Response response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/platform/stores")
            .then()
            .statusCode(Status.CREATED.getStatusCode())
            .body("id", notNullValue())
            .body("storeName", equalTo(storeName))
            .body("subdomain", equalTo(subdomain))
            .body("fullDomain", notNullValue())
            .body("status", notNullValue())
            .body("createdAt", notNullValue())
            .extract().response();
        
        // 3. ASSERT: Verify database state directly
        String storeId = response.jsonPath().getString("id");
        UUID id = UUID.fromString(storeId);
        
        // Get the store directly from the repository
        PlatformStores persistedStore = storeRepository.findById(id);
        
        // Verify the store was properly persisted
        assertNotNull(persistedStore, "Store should be persisted in the database");
        assertEquals(subdomain, persistedStore.getSubdomain(), "Subdomain should match");
        assertEquals(storeName, persistedStore.getStoreName(), "Store name should match");
        assertEquals(StoreStatus.CREATING, persistedStore.getStatus(), "Initial status should be CREATING");
        assertEquals(subdomain + "." + persistedStore.getDomainSuffix(), persistedStore.getFullDomain(), 
                    "Full domain should match subdomain + domain suffix");
        
        // CLEANUP: Delete the test store to prevent test pollution
        storeRepository.deleteById(id);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"Platform Admin"})
    void testCreateStoreEndpoint_RejectsDuplicateSubdomain() {
        // 1. ARRANGE: Set up request data
        AdminUserDetails adminUser = new AdminUserDetails(
            "Integration", 
            "Test", 
            "integration.test@example.com", 
            "test1234"
        );
        
        String subdomain = "duplicate-test-" + System.currentTimeMillis(); // Ensure unique for this test
        
        CreateStoreRequest request = new CreateStoreRequest(
            "First Store", 
            subdomain,
            adminUser
        );

        // 2. ACT & ASSERT: First request succeeds
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/platform/stores")
            .then()
            .statusCode(Status.CREATED.getStatusCode());
        
        // 3. ACT & ASSERT: Second request with same subdomain fails
        CreateStoreRequest duplicateRequest = new CreateStoreRequest(
            "Second Store", 
            subdomain, // Same subdomain
            adminUser
        );
        
        given()
            .contentType(ContentType.JSON)
            .body(duplicateRequest)
            .when()
            .post("/api/v1/platform/stores")
            .then()
            .statusCode(Status.CONFLICT.getStatusCode());
    }

    @Test
    @TestSecurity(user = "user", roles = {"Store Admin"})
    void testCreateStoreEndpointWithWrongRole() {
        AdminUserDetails adminUser = new AdminUserDetails(
            "Integration", 
            "Test", 
            "integration.test@example.com", 
            "test1234"
        );
        
        CreateStoreRequest request = new CreateStoreRequest(
            "Integration Test Store", 
            "integration-test",
            adminUser
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/v1/platform/stores")
            .then()
            .statusCode(Status.FORBIDDEN.getStatusCode());
    }
}
