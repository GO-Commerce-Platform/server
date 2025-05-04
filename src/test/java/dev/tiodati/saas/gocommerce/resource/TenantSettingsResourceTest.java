package dev.tiodati.saas.gocommerce.resource;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.service.TenantSettingsService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

@QuarkusTest
public class TenantSettingsResourceTest {

    @InjectMock
    TenantSettingsService settingsService;
    
    private UUID testTenantId;
    private UUID nonExistentId;
    
    @BeforeEach
    void setUp() {
        testTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testGetSetting_success() {
        // Arrange
        when(settingsService.getSetting(testTenantId, "theme.primaryColor", ""))
            .thenReturn("#FF5722");
        
        // Act & Assert
        given()
            .when()
            .get("/api/admin/tenants/" + testTenantId + "/settings/get?path=theme.primaryColor")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("value", org.hamcrest.Matchers.equalTo("#FF5722"));
        
        verify(settingsService).getSetting(testTenantId, "theme.primaryColor", "");
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testGetSetting_withDefaultValue() {
        // Arrange
        when(settingsService.getSetting(testTenantId, "theme.secondaryColor", "#000000"))
            .thenReturn("#000000");
        
        // Act & Assert
        given()
            .when()
            .get("/api/admin/tenants/" + testTenantId + "/settings/get?path=theme.secondaryColor&default=#000000")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("value", org.hamcrest.Matchers.equalTo("#000000"));
        
        verify(settingsService).getSetting(testTenantId, "theme.secondaryColor", "#000000");
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testGetSetting_missingPath() {
        // Act & Assert
        given()
            .when()
            .get("/api/admin/tenants/" + testTenantId + "/settings/get")
            .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", org.hamcrest.Matchers.equalTo("Path parameter is required"));
        
        verifyNoInteractions(settingsService);
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateSetting_success() {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("path", "theme.primaryColor");
        requestBody.put("value", "#2196F3");
        
        when(settingsService.updateSetting(testTenantId, "theme.primaryColor", "#2196F3"))
            .thenReturn(true);
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants/" + testTenantId + "/settings/update")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("success", org.hamcrest.Matchers.equalTo(true));
        
        verify(settingsService).updateSetting(testTenantId, "theme.primaryColor", "#2196F3");
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateSetting_tenantNotFound() {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("path", "theme.primaryColor");
        requestBody.put("value", "#2196F3");
        
        when(settingsService.updateSetting(nonExistentId, "theme.primaryColor", "#2196F3"))
            .thenReturn(false);
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants/" + nonExistentId + "/settings/update")
            .then()
            .statusCode(404);
        
        verify(settingsService).updateSetting(nonExistentId, "theme.primaryColor", "#2196F3");
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateSetting_missingPath() {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("value", "#2196F3");
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants/" + testTenantId + "/settings/update")
            .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", org.hamcrest.Matchers.equalTo("Path parameter is required"));
        
        verifyNoInteractions(settingsService);
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testUpdateSetting_missingValue() {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("path", "theme.primaryColor");
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants/" + testTenantId + "/settings/update")
            .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", org.hamcrest.Matchers.equalTo("Value parameter is required"));
        
        verifyNoInteractions(settingsService);
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testBulkUpdateSettings_success() {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("theme.primaryColor", "#2196F3");
        requestBody.put("theme.secondaryColor", "#4CAF50");
        
        when(settingsService.updateSettings(testTenantId, requestBody))
            .thenReturn(true);
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants/" + testTenantId + "/settings/bulk-update")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("success", org.hamcrest.Matchers.equalTo(true));
        
        verify(settingsService).updateSettings(testTenantId, requestBody);
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testBulkUpdateSettings_tenantNotFound() {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("theme.primaryColor", "#2196F3");
        
        when(settingsService.updateSettings(nonExistentId, requestBody))
            .thenReturn(false);
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants/" + nonExistentId + "/settings/bulk-update")
            .then()
            .statusCode(404);
        
        verify(settingsService).updateSettings(nonExistentId, requestBody);
    }
    
    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    void testBulkUpdateSettings_emptySettings() {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/admin/tenants/" + testTenantId + "/settings/bulk-update")
            .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", org.hamcrest.Matchers.equalTo("Settings map cannot be empty"));
        
        verifyNoInteractions(settingsService);
    }
}