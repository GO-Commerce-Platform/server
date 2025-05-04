package dev.tiodati.saas.gocommerce.resource;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import dev.tiodati.saas.gocommerce.util.PasswordHashingUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
public class TenantResourcePasswordTest {

    @InjectMock
    TenantService tenantService;
    
    @Inject
    PasswordHashingUtil passwordHashingUtil;
    
    private Tenant testTenant;
    private UUID testTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    
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
    void testCreateTenant_withPasswordHashing() {
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
        
        UUID newTenantId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        
        // Setup tenant creation mock to capture the admin object
        when(tenantService.createTenant(any(Tenant.class), any(TenantAdmin.class)))
                .thenAnswer(invocation -> {
                    Tenant tenant = invocation.getArgument(0);
                    TenantAdmin admin = invocation.getArgument(1);
                    
                    // Set the ID to simulate persistence
                    tenant.setId(newTenantId);
                    
                    // For verification
                    // Verify password was hashed (not stored as plaintext)
                    assert !admin.getPasswordHash().equals("password123") : "Password should be hashed";
                    
                    // Verify the hashed password is valid and can be verified
                    assert passwordHashingUtil.verifyPassword("password123", admin.getPasswordHash()) : 
                           "Hashed password should be verifiable";
                    
                    return tenant;
                });
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants")
            .then()
            .statusCode(201);
            
        // Verify service was called
        verify(tenantService).createTenant(any(Tenant.class), any(TenantAdmin.class));
    }
}