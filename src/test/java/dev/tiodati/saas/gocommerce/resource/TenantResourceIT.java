package dev.tiodati.saas.gocommerce.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.model.tenant.Tenant;
import dev.tiodati.saas.gocommerce.model.tenant.TenantAdmin;
import dev.tiodati.saas.gocommerce.model.tenant.TenantStatus;
import dev.tiodati.saas.gocommerce.service.TenantService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

@QuarkusTest
public class TenantResourceIT {

    @InjectMock
    TenantService tenantService;
    
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        // Setup test data
        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setTenantKey("test-tenant");
        testTenant.setName("Test Tenant");
        testTenant.setSubdomain("test");
        testTenant.setStatus(TenantStatus.ACTIVE);
        testTenant.setBillingPlan("BASIC");
        testTenant.setSchemaName("tenant_test-tenant");
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testListTenants() {
        // Arrange
        Tenant tenant2 = new Tenant();
        tenant2.setId(2L);
        tenant2.setTenantKey("tenant2");
        tenant2.setName("Tenant 2");
        tenant2.setSubdomain("tenant2");
        tenant2.setStatus(TenantStatus.TRIAL);
        tenant2.setBillingPlan("PREMIUM");
        
        when(tenantService.listAll()).thenReturn(Arrays.asList(testTenant, tenant2));
        
        // Act & Assert
        given()
            .when()
            .get("/api/admin/tenants")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body("[0].id", is(1))
            .body("[0].tenantKey", is("test-tenant"))
            .body("[0].name", is("Test Tenant"))
            .body("[1].id", is(2))
            .body("[1].tenantKey", is("tenant2"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testGetTenant() {
        // Arrange
        when(tenantService.findById(1L)).thenReturn(Optional.of(testTenant));
        
        // Act & Assert
        given()
            .when()
            .get("/api/admin/tenants/1")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", is(1))
            .body("tenantKey", is("test-tenant"))
            .body("name", is("Test Tenant"))
            .body("subdomain", is("test"))
            .body("status", is("ACTIVE"))
            .body("billingPlan", is("BASIC"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testGetTenant_notFound() {
        // Arrange
        when(tenantService.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        given()
            .when()
            .get("/api/admin/tenants/999")
            .then()
            .statusCode(404);
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testCreateTenant() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tenantKey", "new-tenant");
        requestBody.put("name", "New Tenant");
        requestBody.put("subdomain", "new");
        requestBody.put("billingPlan", "PREMIUM");
        requestBody.put("adminEmail", "admin@newtenant.com");
        requestBody.put("adminPassword", "password123");
        requestBody.put("adminFirstName", "Admin");
        requestBody.put("adminLastName", "User");
        
        when(tenantService.findByTenantKey("new-tenant")).thenReturn(Optional.empty());
        when(tenantService.findBySubdomain("new")).thenReturn(Optional.empty());
        
        Tenant createdTenant = new Tenant();
        createdTenant.setId(3L);
        createdTenant.setTenantKey("new-tenant");
        createdTenant.setName("New Tenant");
        createdTenant.setSubdomain("new");
        createdTenant.setStatus(TenantStatus.TRIAL);
        createdTenant.setBillingPlan("PREMIUM");
        
        when(tenantService.createTenant(any(Tenant.class), any(TenantAdmin.class)))
                .thenReturn(createdTenant);
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants")
            .then()
            .statusCode(201)
            .header("Location", is("/api/admin/tenants/3"))
            .contentType(ContentType.JSON)
            .body("id", is(3))
            .body("tenantKey", is("new-tenant"))
            .body("name", is("New Tenant"))
            .body("subdomain", is("new"))
            .body("status", is("TRIAL"))
            .body("billingPlan", is("PREMIUM"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testCreateTenant_tenantKeyAlreadyExists() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tenantKey", "existing-key");
        requestBody.put("name", "New Tenant");
        requestBody.put("subdomain", "new");
        requestBody.put("adminEmail", "admin@test.com");
        requestBody.put("adminPassword", "password123");
        requestBody.put("adminFirstName", "Admin");
        requestBody.put("adminLastName", "User");
        
        when(tenantService.findByTenantKey("existing-key")).thenReturn(Optional.of(new Tenant()));
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants")
            .then()
            .statusCode(409)
            .contentType(ContentType.JSON)
            .body("error", is("Tenant key already exists"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testCreateTenant_subdomainAlreadyExists() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tenantKey", "new-tenant-key");
        requestBody.put("name", "New Tenant");
        requestBody.put("subdomain", "existing-subdomain");
        requestBody.put("adminEmail", "admin@test.com");
        requestBody.put("adminPassword", "password123");
        requestBody.put("adminFirstName", "Admin");
        requestBody.put("adminLastName", "User");
        
        when(tenantService.findByTenantKey("new-tenant-key")).thenReturn(Optional.empty());
        when(tenantService.findBySubdomain("existing-subdomain")).thenReturn(Optional.of(new Tenant()));
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants")
            .then()
            .statusCode(409)
            .contentType(ContentType.JSON)
            .body("error", is("Subdomain already exists"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testCreateTenant_missingRequiredFields() {
        // Arrange: Missing tenantKey, name, subdomain, adminEmail, adminPassword
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("adminFirstName", "Admin");
        requestBody.put("adminLastName", "User");

        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants")
            .then()
            .statusCode(400); // Expect Bad Request due to validation failures
            // We could add more specific assertions on the validation error messages if needed
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateTenant() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", 1);
        requestBody.put("tenantKey", "test-tenant");  // Cannot be changed
        requestBody.put("name", "Updated Name");
        requestBody.put("subdomain", "test");
        requestBody.put("status", "INACTIVE");
        requestBody.put("billingPlan", "PREMIUM");
        
        Tenant updatedTenant = new Tenant();
        updatedTenant.setId(1L);
        updatedTenant.setTenantKey("test-tenant");
        updatedTenant.setName("Updated Name");
        updatedTenant.setSubdomain("test");
        updatedTenant.setStatus(TenantStatus.INACTIVE);
        updatedTenant.setBillingPlan("PREMIUM");
        
        when(tenantService.findById(1L)).thenReturn(Optional.of(testTenant));
        when(tenantService.updateTenant(any(Tenant.class))).thenReturn(updatedTenant);
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .put("/api/admin/tenants/1")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", is(1))
            .body("name", is("Updated Name"))
            .body("status", is("INACTIVE"))
            .body("billingPlan", is("PREMIUM"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateTenant_notFound() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", 999);
        requestBody.put("name", "Update Attempt");
        
        when(tenantService.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .put("/api/admin/tenants/999")
            .then()
            .statusCode(404);
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateTenant_subdomainAlreadyExists() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", 1);
        requestBody.put("tenantKey", "test-tenant");
        requestBody.put("name", "Updated Name");
        requestBody.put("subdomain", "existing-subdomain"); // Try to update to an existing subdomain
        requestBody.put("status", "ACTIVE");
        requestBody.put("billingPlan", "BASIC");
        
        Tenant otherTenant = new Tenant();
        otherTenant.setId(2L);
        otherTenant.setSubdomain("existing-subdomain");
        
        when(tenantService.findById(1L)).thenReturn(Optional.of(testTenant)); // Original tenant exists
        when(tenantService.findBySubdomain("existing-subdomain")).thenReturn(Optional.of(otherTenant)); // Subdomain taken
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .put("/api/admin/tenants/1")
            .then()
            .statusCode(409)
            .contentType(ContentType.JSON)
            .body("error", is("Subdomain already exists"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateTenantStatus() {
        // Arrange
        Map<String, String> requestBody = Collections.singletonMap("status", "INACTIVE");
        
        Tenant updatedTenant = new Tenant();
        updatedTenant.setId(1L);
        updatedTenant.setTenantKey("test-tenant");
        updatedTenant.setName("Test Tenant");
        updatedTenant.setStatus(TenantStatus.INACTIVE);
        
        when(tenantService.updateTenantStatus(1L, TenantStatus.INACTIVE)).thenReturn(updatedTenant);
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .patch("/api/admin/tenants/1/status")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("status", is("INACTIVE"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateTenantStatus_invalidStatus() {
        // Arrange
        Map<String, String> requestBody = Collections.singletonMap("status", "INVALID_STATUS");
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .patch("/api/admin/tenants/1/status")
            .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", is("Invalid status value"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateTenantStatus_notFound() {
        // Arrange
        Map<String, String> requestBody = Collections.singletonMap("status", "INACTIVE");
        
        // Simulate service throwing exception or returning empty/null when tenant not found
        // The resource translates this to a 404 or appropriate error
        when(tenantService.updateTenantStatus(999L, TenantStatus.INACTIVE))
            .thenThrow(new jakarta.ws.rs.NotFoundException("Tenant not found"));
            
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .patch("/api/admin/tenants/999/status")
            .then()
            .statusCode(404);
    }

    @Test
    void testEndpointsWithoutAuthentication() {
        // List tenants without authentication
        given()
            .when()
            .get("/api/admin/tenants")
            .then()
            .statusCode(401);
            
        // Get tenant without authentication
        given()
            .when()
            .get("/api/admin/tenants/1")
            .then()
            .statusCode(401);
            
        // Create tenant without authentication
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tenantKey", "new-tenant");
        requestBody.put("name", "New Tenant");
        
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants")
            .then()
            .statusCode(401);
    }
}