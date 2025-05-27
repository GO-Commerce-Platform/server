package dev.tiodati.saas.gocommerce.platform.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.platform.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.auth.dto.LoginRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class PlatformAdminResourceTest {

    /**
     * Base path for platform admin endpoints.
     */
    private static final String PLATFORM_ADMIN_BASE_PATH = "/api/platform";

    @Test
    public void testCreateStoreEndpoint() {
        // Generate a unique subdomain for each test run
        String uniqueSubdomain = "test-store-" + UUID.randomUUID().toString();
        CreateStoreRequest request = new CreateStoreRequest("Test Store", "test-store", uniqueSubdomain + "@example.com",
                "USD", "en-US", null, "This is a test store");

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + getPlatformAdminToken()) // Assuming a method to get a valid token
            .body(request)
        .when()
            .post(PLATFORM_ADMIN_BASE_PATH + "/stores")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("Test Store"))
            .body("subdomain", equalTo(uniqueSubdomain));
    }

    // Helper method to obtain a platform admin token (mocked or real)
    // This is a placeholder; actual implementation will depend on your auth setup
    private String getPlatformAdminToken() {
        // In a real test, you would obtain a valid token, possibly by calling the login endpoint
        // For now, returning a placeholder. This needs to be a valid JWT for security checks to pass.
        // If Keycloak is running and configured for tests, you might programmatically get a token.
        // For simplicity, if tests run without security enabled or with a mock, this might be simpler.
        // This part is crucial for tests requiring authentication.
        LoginRequest loginRequest = new LoginRequest("platformadmin", "adminpass"); // Example credentials

        String token = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/api/v1/auth/login") // Assuming AuthResourceTest changes are applied
        .then()
            .statusCode(200)
            .extract().path("accessToken");
        return token;
        // return "mocked-platform-admin-jwt-token";
    }
}
