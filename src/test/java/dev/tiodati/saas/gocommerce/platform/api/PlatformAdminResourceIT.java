package dev.tiodati.saas.gocommerce.platform.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo; // Import Matchers.equalTo

import java.util.UUID; // Import UUID

import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.auth.dto.LoginRequest;
import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.store.entity.StoreStatus; // Corrected import for StoreStatus
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response.Status;

/**
 * Integration tests for the PlatformAdminResource endpoints.
 * These tests ensure that only users with the PLATFORM_ADMIN role can create stores.
 */
@QuarkusTest
public class PlatformAdminResourceIT {

    @Test
    void testCreateStoreEndpointWithProperRole() {
        String ownerId = UUID.randomUUID().toString();
        String uniqueSubdomain = "itest-proper-" + ownerId.substring(0, 8);

        CreateStoreRequest request = new CreateStoreRequest(
                "Integration Test Store", // name
                uniqueSubdomain, // subdomain
                ownerId, // ownerId
                "integration.test@example.com", // email
                "USD", // currencyCode
                "en-US", // defaultLocale
                StoreStatus.ACTIVE, // status
                "Integration test store description." // description
        );

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + getPlatformAdminToken("platform-admin", "platform-admin"))
                .body(request).when()
                .post("/api/v1/platform/stores").then()
                .statusCode(Status.CREATED.getStatusCode())
                .body("id", notNullValue())
                .body("name", equalTo(request.name()))
                .body("subdomain", equalTo(request.subdomain()))
                .body("ownerId", equalTo(ownerId))
                .body("fullDomain", notNullValue())
                .body("status", equalTo(request.status().toString()))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }

    @Test
    void testCreateStoreEndpointWithWrongRole() {
        String ownerId = UUID.randomUUID().toString();
        String uniqueSubdomain = "itest-proper-" + ownerId.substring(0, 8);

        CreateStoreRequest request = new CreateStoreRequest(
                "Integration Test Store", // name
                uniqueSubdomain, // subdomain
                ownerId, // ownerId
                "integration.test@example.com", // email
                "USD", // currencyCode
                "en-US", // defaultLocale
                StoreStatus.ACTIVE, // status
                "Integration test store description." // description
        );

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + getPlatformAdminToken("store-admin", "store-admin"))
                .body(request).when()
                .post("/api/v1/platform/stores").then()
                .statusCode(Status.FORBIDDEN.getStatusCode());
    }

    // Helper method to obtain a platform admin token (mocked or real)
    // This is a placeholder; actual implementation will depend on your auth
    // setup
    private String getPlatformAdminToken(String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);

        String token = given().contentType(ContentType.JSON).body(loginRequest)
                .when().post("/api/v1/auth/login")
                .then().statusCode(200).extract().path("access_token");
        return token;
    }
}
