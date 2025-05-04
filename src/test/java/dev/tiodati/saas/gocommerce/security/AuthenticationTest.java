package dev.tiodati.saas.gocommerce.security;

import static io.restassured.RestAssured.given;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

/**
 * Tests authentication functionality using TestSecurity annotation
 * instead of direct Keycloak integration.
 */
@QuarkusTest
public class AuthenticationTest {

    private UUID testTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    
    /**
     * Tests that an admin user can access protected resources
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testAdminAuthentication() {
        // Access an admin endpoint with simulated admin authentication
        given()
            .when()
            .get("/api/admin/tenants")
            .then()
            .statusCode(200);
    }
    
    /**
     * Tests that non-authenticated requests are rejected
     */
    @Test
    public void testUnauthenticatedAccess() {
        // Attempt to access protected endpoints without authentication
        given()
            .when()
            .get("/api/admin/tenants")
            .then()
            .statusCode(401); // Expect Unauthorized response
    }
    
    /**
     * Tests access to tenant settings with admin role
     */
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testSecuredEndpointAccess() {
        given()
            .when()
            .get("/api/admin/tenants/" + testTenantId + "/settings/get?path=theme.primaryColor")
            .then()
            .statusCode(200);
    }
    
    /**
     * Tests rejection with wrong role
     */
    @Test
    @TestSecurity(user = "regular-user", roles = {"user"})
    public void testInsufficientRoleAccess() {
        // Regular user cannot access admin endpoints
        given()
            .when()
            .get("/api/admin/tenants")
            .then()
            .statusCode(403); // Forbidden
    }
}
