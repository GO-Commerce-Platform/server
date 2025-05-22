package dev.tiodati.saas.gocommerce.platform.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.platform.api.dto.AdminUserDetails;
import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response.Status;

@QuarkusIntegrationTest
public class PlatformAdminResourceIT {

    @Test
    @TestSecurity(user = "admin", roles = {"Platform Admin"})
    void testCreateStoreEndpointWithProperRole() {
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
            .statusCode(Status.CREATED.getStatusCode())
            .body("id", notNullValue())
            .body("name", notNullValue())
            .body("subdomain", notNullValue())
            .body("fullDomain", notNullValue())
            .body("status", notNullValue())
            .body("createdAt", notNullValue());
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
