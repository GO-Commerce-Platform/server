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
import java.util.UUID;

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
    private UUID testTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private UUID tenant2Id = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private UUID tenant3Id = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        // Setup test data
        testTenant = new Tenant();
        testTenant.setId(testTenantId);
        testTenant.setTenantKey("test-tenant");
        testTenant.setName("Test Tenant");
        testTenant.setSubdomain("test");
        testTenant.setStatus(TenantStatus.ACTIVE);
        testTenant.setBillingPlan("BASIC");
        testTenant.setSchemaName("tenant_test-tenant");
        testTenant.setDeleted(false);
        testTenant.setVersion(1);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testListTenants() {
        // Arrange
        Tenant tenant2 = new Tenant();
        tenant2.setId(tenant2Id);
        tenant2.setTenantKey("tenant2");
        tenant2.setName("Tenant 2");
        tenant2.setSubdomain("tenant2");
        tenant2.setStatus(TenantStatus.TRIAL);
        tenant2.setBillingPlan("PREMIUM");
        tenant2.setDeleted(false);
        tenant2.setVersion(1);
        
        when(tenantService.listAll()).thenReturn(Arrays.asList(testTenant, tenant2));
        
        // Act & Assert
        given()
            .when()
            .get("/api/admin/tenants")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body("[0].id", is(testTenantId.toString()))
            .body("[0].tenantKey", is("test-tenant"))
            .body("[0].name", is("Test Tenant"))
            .body("[1].id", is(tenant2Id.toString()))
            .body("[1].tenantKey", is("tenant2"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testGetTenant() {
        // Arrange
        when(tenantService.findById(testTenantId)).thenReturn(Optional.of(testTenant));
        
        // Act & Assert
        given()
            .when()
            .get("/api/admin/tenants/" + testTenantId)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", is(testTenantId.toString()))
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
        UUID nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(tenantService.findById(nonExistentId)).thenReturn(Optional.empty());
        
        // Act & Assert
        given()
            .when()
            .get("/api/admin/tenants/" + nonExistentId)
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
        createdTenant.setId(tenant3Id);
        createdTenant.setTenantKey("new-tenant");
        createdTenant.setName("New Tenant");
        createdTenant.setSubdomain("new");
        createdTenant.setStatus(TenantStatus.TRIAL);
        createdTenant.setBillingPlan("PREMIUM");
        createdTenant.setDeleted(false);
        createdTenant.setVersion(1);
        
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
            .header("Location", is("/api/admin/tenants/" + tenant3Id))
            .contentType(ContentType.JSON)
            .body("id", is(tenant3Id.toString()))
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
        requestBody.put("id", testTenantId.toString());
        requestBody.put("tenantKey", "test-tenant");  // Cannot be changed
        requestBody.put("name", "Updated Name");
        requestBody.put("subdomain", "test");
        requestBody.put("status", "INACTIVE");
        requestBody.put("billingPlan", "PREMIUM");
        
        Tenant updatedTenant = new Tenant();
        updatedTenant.setId(testTenantId);
        updatedTenant.setTenantKey("test-tenant");
        updatedTenant.setName("Updated Name");
        updatedTenant.setSubdomain("test");
        updatedTenant.setStatus(TenantStatus.INACTIVE);
        updatedTenant.setBillingPlan("PREMIUM");
        updatedTenant.setDeleted(false);
        updatedTenant.setVersion(2);
        
        when(tenantService.findById(testTenantId)).thenReturn(Optional.of(testTenant));
        when(tenantService.updateTenant(any(Tenant.class))).thenReturn(updatedTenant);
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .put("/api/admin/tenants/" + testTenantId)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", is(testTenantId.toString()))
            .body("name", is("Updated Name"))
            .body("status", is("INACTIVE"))
            .body("billingPlan", is("PREMIUM"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateTenant_notFound() {
        // Arrange
        UUID nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", nonExistentId.toString());
        requestBody.put("name", "Update Attempt");
        
        when(tenantService.findById(nonExistentId)).thenReturn(Optional.empty());
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .put("/api/admin/tenants/" + nonExistentId)
            .then()
            .statusCode(404);
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateTenant_subdomainAlreadyExists() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", testTenantId.toString());
        requestBody.put("tenantKey", "test-tenant");
        requestBody.put("name", "Updated Name");
        requestBody.put("subdomain", "existing-subdomain"); // Try to update to an existing subdomain
        requestBody.put("status", "ACTIVE");
        requestBody.put("billingPlan", "BASIC");
        
        Tenant otherTenant = new Tenant();
        otherTenant.setId(tenant2Id);
        otherTenant.setSubdomain("existing-subdomain");
        
        when(tenantService.findById(testTenantId)).thenReturn(Optional.of(testTenant)); // Original tenant exists
        when(tenantService.findBySubdomain("existing-subdomain")).thenReturn(Optional.of(otherTenant)); // Subdomain taken
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .put("/api/admin/tenants/" + testTenantId)
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
        updatedTenant.setId(testTenantId);
        updatedTenant.setTenantKey("test-tenant");
        updatedTenant.setName("Test Tenant");
        updatedTenant.setStatus(TenantStatus.INACTIVE);
        updatedTenant.setDeleted(false);
        updatedTenant.setVersion(2);
        
        when(tenantService.updateTenantStatus(testTenantId, TenantStatus.INACTIVE)).thenReturn(updatedTenant);
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .patch("/api/admin/tenants/" + testTenantId + "/status")
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
            .patch("/api/admin/tenants/" + testTenantId + "/status")
            .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", is("Invalid status value"));
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateTenantStatus_notFound() {
        // Arrange
        UUID nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        Map<String, String> requestBody = Collections.singletonMap("status", "INACTIVE");
        
        // Simulate service throwing exception or returning empty/null when tenant not found
        when(tenantService.updateTenantStatus(nonExistentId, TenantStatus.INACTIVE))
            .thenThrow(new jakarta.ws.rs.NotFoundException("Tenant not found"));
            
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .patch("/api/admin/tenants/" + nonExistentId + "/status")
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
            .get("/api/admin/tenants/" + testTenantId)
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