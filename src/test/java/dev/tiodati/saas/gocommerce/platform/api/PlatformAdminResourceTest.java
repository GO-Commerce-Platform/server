package dev.tiodati.saas.gocommerce.platform.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.platform.api.dto.AdminUserDetails;
import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.entity.StoreStatus;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response.Status;

@QuarkusTest
public class PlatformAdminResourceTest {

    /**
     * Represents the admin user details for testing.
     */
    private AdminUserDetails adminUser;
    /**
     * Represents a valid request to create a store for testing.
     */
    private CreateStoreRequest validRequest;

    @BeforeEach
    void setUp() {
        adminUser = new AdminUserDetails("John", "Doe", "john.doe@example.com",
                "password123");

        validRequest = new CreateStoreRequest("Test Store", // name
                "test-store", // subdomain
                adminUser.email(), // email
                "USD", // currencyCode
                "en-US", // defaultLocale
                StoreStatus.ACTIVE, // status
                "A test store description." // description
        );
    }

    @Test
    @TestSecurity(user = "admin", roles = { "Platform Admin" })
    void testCreateStoreEndpoint() {
        // Since we're using real implementation instead of mocks,
        // we need to ensure all data is properly se

        given().contentType(ContentType.JSON).body(validRequest).when()
                .post("/api/v1/platform/stores").then().log()
                .ifValidationFails() // Add logging for debugging
                .statusCode(Status.CREATED.getStatusCode())
                .body("id", notNullValue()).body("name", is("Test Store"))
                .body("subdomain", is("test-store"))
                .body("fullDomain", is("test-store.gocommerce.com"))
                .body("status", is("ACTIVE")).body("createdAt", notNullValue());
    }

    @Test
    @TestSecurity(user = "admin", roles = { "Platform Admin" })
    void testCreateStoreWithInvalidData() {
        // Create request with invalid data (empty store name)
        CreateStoreRequest invalidRequest = new CreateStoreRequest("", // Empty
                                                                       // name -
                                                                       // should
                                                                       // fail
                                                                       // validation
                "test-store", // subdomain
                adminUser.email(), // email
                "USD", // currencyCode
                "en-US", // defaultLocale
                StoreStatus.ACTIVE, // status
                "A test store description." // description
        );

        given().contentType(ContentType.JSON).body(invalidRequest).when()
                .post("/api/v1/platform/stores").then()
                .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void testCreateStoreWithoutAuthentication() {
        given().contentType(ContentType.JSON).body(validRequest).when()
                .post("/api/v1/platform/stores").then()
                .statusCode(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @TestSecurity(user = "store-admin", roles = { "Store Admin" })
    void testCreateStoreWithInsufficientRole() {
        given().contentType(ContentType.JSON).body(validRequest).when()
                .post("/api/v1/platform/stores").then()
                .statusCode(Status.FORBIDDEN.getStatusCode());
    }
}
