package dev.tiodati.saas.gocommerce.customer.resource;

import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import dev.tiodati.saas.gocommerce.store.StoreContext;
import dev.tiodati.saas.gocommerce.store.entity.Store;
import dev.tiodati.saas.gocommerce.store.entity.StoreStatus;
import dev.tiodati.saas.gocommerce.testinfra.MultiTenantTest;
import dev.tiodati.saas.gocommerce.testinfra.TestTenantContext;
import dev.tiodati.saas.gocommerce.testinfra.TestTenantResolver;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@MultiTenantTest
@TestSecurity(user = "test-user", roles = { "PLATFORM_ADMIN" })
class CustomerResourceTest {

    // This field will be automatically injected by MultiTenantTestExtension
    private String testSchemaName;

    @Inject
    TestTenantContext testTenantContext;

    private Store testStore;

    @BeforeEach
    @Transactional
    void setupStore() {
        // Get the current tenant from TestTenantContext directly instead of relying on injection
        String currentTenant = testTenantContext.getCurrentTenant();
        System.out.println("DEBUG: testSchemaName field = " + testSchemaName);
        System.out.println("DEBUG: currentTenant from context = " + currentTenant);
        
        // Use the current tenant as schema name (it should be the same)
        String schemaName = currentTenant != null ? currentTenant : testSchemaName;
        System.out.println("DEBUG: Using schema name = " + schemaName);
        
        // Create a test store that corresponds to the schema being set up by the test extension
        String storeKey = "teststore" + UUID.randomUUID().toString().substring(0, 8);
        testStore = new Store();
        // Don't set ID - it's auto-generated
        testStore.setStoreKey(storeKey);
        testStore.setName("Test Store");
        testStore.setSubdomain("test-subdomain-" + storeKey);
        testStore.setEmail("admin@teststore.com");
        testStore.setStatus(StoreStatus.ACTIVE);
        testStore.setBillingPlan("FREE");
        testStore.setCurrencyCode("USD");
        testStore.setDefaultLocale("en");
        testStore.setSchemaName(schemaName); // Link to the schema created by MultiTenantTestExtension
        // Don't set createdAt, updatedAt, or version - they're managed by JPA
        
        System.out.println("DEBUG: Store schema name set to: " + testStore.getSchemaName());
        
        // Store is a platform-level entity, so temporarily clear tenant context to persist it in main schema
        String originalTenant = testTenantContext.getCurrentTenant();
        testTenantContext.clear();
        // Note: Don't need to clear StoreContext in tests - use TestTenantContext exclusively
        try {
            testStore.persist();
            System.out.println("DEBUG: Store persisted with ID: " + testStore.getId());
        } finally {
            // Restore tenant context
            if (originalTenant != null) {
                testTenantContext.setCurrentTenant(originalTenant);
            }
        }
    }

    @Test
    void testCreateCustomer() {
        String currentTenant = testTenantContext.getCurrentTenant();
        System.out.println("DEBUG: Test method - currentTenant = " + currentTenant);
        
        CreateCustomerDto createDto = new CreateCustomerDto(
            "test@example.com", 
            "Test", 
            "User", 
            null, null, null, null, null, null, null, null, null, 
            false, 
            "en"
        );

        given()
                .contentType(ContentType.JSON)
                .header(TestTenantResolver.TENANT_HEADER, currentTenant) // Set the tenant for the HTTP request
                .body(createDto)
                .when()
                .post("/api/v1/stores/{storeId}/customers", testStore.getId())
                .then()
                .statusCode(201)
                .body("email", is(createDto.email()));
    }

}
