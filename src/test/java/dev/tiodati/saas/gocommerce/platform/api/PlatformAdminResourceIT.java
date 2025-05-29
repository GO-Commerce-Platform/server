package dev.tiodati.saas.gocommerce.platform.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo; // Import Matchers.equalTo

import java.util.UUID; // Import UUID

import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.platform.api.dto.AdminUserDetails;
import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.store.entity.StoreStatus; // Corrected import for StoreStatus
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response.Status;

@QuarkusIntegrationTest
public class PlatformAdminResourceIT {

    @Test
    @TestSecurity(user = "admin", roles = { "PLATFORM_ADMIN" }) // Changed
                                                                // "Platform
                                                                // Admin" to
                                                                // "PLATFORM_ADMIN"
    void testCreateStoreEndpointWithProperRole() {
        AdminUserDetails adminUser = new AdminUserDetails("Integration", "Test",
                "integration.test@example.com", "test1234");
        String ownerId = UUID.randomUUID().toString();
        String uniqueSubdomain = "itest-proper-"
                + UUID.randomUUID().toString().substring(0, 8);

        CreateStoreRequest request = new CreateStoreRequest(
                "Integration Test Store", // name
                uniqueSubdomain, // subdomain
                ownerId, // ownerId
                adminUser.email(), // email
                "USD", // currencyCode
                "en-US", // defaultLocale
                StoreStatus.ACTIVE, // status
                "Integration test store description." // description
        );

        given().contentType(ContentType.JSON).body(request).when()
                .post("/api/v1/platform/stores").then()
                .statusCode(Status.CREATED.getStatusCode())
                .body("id", notNullValue())
                .body("name", equalTo(request.name()))
                .body("subdomain", equalTo(request.subdomain()))
                .body("ownerId", equalTo(ownerId))
                .body("fullDomain", notNullValue()) // Consider specific
                                                    // assertion if format is
                                                    // known
                .body("status", equalTo(request.status().toString()))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }

    @Test
    @TestSecurity(user = "user", roles = { "STORE_ADMIN" }) // Changed "Store
                                                            // Admin" to
                                                            // "STORE_ADMIN"
    void testCreateStoreEndpointWithWrongRole() {
        AdminUserDetails adminUser = new AdminUserDetails("Integration", "Test",
                "integration.test.wrongrole@example.com", "test1234");
        String ownerId = UUID.randomUUID().toString();
        String uniqueSubdomain = "itest-wrongrole-"
                + UUID.randomUUID().toString().substring(0, 8);

        CreateStoreRequest request = new CreateStoreRequest(
                "Integration Test Store Wrong Role", // name
                uniqueSubdomain, // subdomain
                ownerId, // ownerId
                adminUser.email(), // email
                "USD", // currencyCode
                "en-US", // defaultLocale
                StoreStatus.PENDING, // status
                "Store creation attempt with wrong role." // description
        );

        given().contentType(ContentType.JSON).body(request).when()
                .post("/api/v1/platform/stores").then()
                .statusCode(Status.FORBIDDEN.getStatusCode());
    }
}
