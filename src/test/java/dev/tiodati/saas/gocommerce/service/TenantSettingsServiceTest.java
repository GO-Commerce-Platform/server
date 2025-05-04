package dev.tiodati.saas.gocommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.model.tenant.Tenant;
import dev.tiodati.saas.gocommerce.util.TenantSettingsUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class TenantSettingsServiceTest {

    @Inject
    TenantSettingsService tenantSettingsService;
    
    @InjectMock
    TenantService tenantService;
    
    @InjectMock
    TenantSettingsUtil settingsUtil;
    
    private Tenant testTenant;
    private UUID testTenantId;
    private UUID nonExistentId;
    
    @BeforeEach
    void setUp() {
        testTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        
        // Set up test tenant
        testTenant = new Tenant();
        testTenant.setId(testTenantId);
        testTenant.setName("Test Tenant");
        testTenant.setTenantKey("test-tenant");
        testTenant.setSubdomain("test");
        testTenant.setSettings("{\"theme\":{\"primaryColor\":\"#FF5722\"}}");
        testTenant.setDeleted(false);
        testTenant.setVersion(1);
    }
    
    @Test
    void testGetSetting_whenTenantExists() {
        // Arrange
        when(tenantService.findById(testTenantId)).thenReturn(Optional.of(testTenant));
        when(settingsUtil.getSetting(testTenant.getSettings(), "theme.primaryColor", "default"))
            .thenReturn("#FF5722");
        
        // Act
        String result = tenantSettingsService.getSetting(testTenantId, "theme.primaryColor", "default");
        
        // Assert
        assertEquals("#FF5722", result);
        verify(tenantService).findById(testTenantId);
        verify(settingsUtil).getSetting(testTenant.getSettings(), "theme.primaryColor", "default");
    }
    
    @Test
    void testGetSetting_whenTenantDoesNotExist() {
        // Arrange
        when(tenantService.findById(nonExistentId)).thenReturn(Optional.empty());
        
        // Act
        String result = tenantSettingsService.getSetting(nonExistentId, "theme.primaryColor", "default");
        
        // Assert
        assertEquals("default", result);
        verify(tenantService).findById(nonExistentId);
        verifyNoInteractions(settingsUtil);
    }
    
    @Test
    void testUpdateSetting_success() {
        // Arrange
        when(tenantService.findById(testTenantId)).thenReturn(Optional.of(testTenant));
        String updatedSettings = "{\"theme\":{\"primaryColor\":\"#2196F3\"}}";
        when(settingsUtil.updateSetting(testTenant.getSettings(), "theme.primaryColor", "#2196F3"))
            .thenReturn(updatedSettings);
        
        // Act
        boolean result = tenantSettingsService.updateSetting(testTenantId, "theme.primaryColor", "#2196F3");
        
        // Assert
        assertTrue(result);
        assertEquals(updatedSettings, testTenant.getSettings());
        verify(tenantService).findById(testTenantId);
        verify(tenantService).updateTenant(testTenant);
        verify(settingsUtil).updateSetting(anyString(), eq("theme.primaryColor"), eq("#2196F3"));
    }
    
    @Test
    void testUpdateSetting_tenantNotFound() {
        // Arrange
        when(tenantService.findById(nonExistentId)).thenReturn(Optional.empty());
        
        // Act
        boolean result = tenantSettingsService.updateSetting(nonExistentId, "theme.primaryColor", "#2196F3");
        
        // Assert
        assertFalse(result);
        verify(tenantService).findById(nonExistentId);
        verifyNoInteractions(settingsUtil);
        verify(tenantService, never()).updateTenant(any());
    }
    
    @Test
    void testUpdateSettings_success() {
        // Arrange
        when(tenantService.findById(testTenantId)).thenReturn(Optional.of(testTenant));
        
        Map<String, String> settingsToUpdate = new HashMap<>();
        settingsToUpdate.put("theme.primaryColor", "#2196F3");
        settingsToUpdate.put("theme.secondaryColor", "#4CAF50");
        
        // First update
        String intermediateSettings = "{\"theme\":{\"primaryColor\":\"#2196F3\"}}";
        when(settingsUtil.updateSetting(testTenant.getSettings(), "theme.primaryColor", "#2196F3"))
            .thenReturn(intermediateSettings);
        
        // Second update
        String finalSettings = "{\"theme\":{\"primaryColor\":\"#2196F3\",\"secondaryColor\":\"#4CAF50\"}}";
        when(settingsUtil.updateSetting(intermediateSettings, "theme.secondaryColor", "#4CAF50"))
            .thenReturn(finalSettings);
        
        // Act
        boolean result = tenantSettingsService.updateSettings(testTenantId, settingsToUpdate);
        
        // Assert
        assertTrue(result);
        assertEquals(finalSettings, testTenant.getSettings());
        verify(tenantService).findById(testTenantId);
        verify(tenantService).updateTenant(testTenant);
    }
    
    @Test
    void testUpdateSettings_tenantNotFound() {
        // Arrange
        when(tenantService.findById(nonExistentId)).thenReturn(Optional.empty());
        
        Map<String, String> settingsToUpdate = new HashMap<>();
        settingsToUpdate.put("theme.primaryColor", "#2196F3");
        
        // Act
        boolean result = tenantSettingsService.updateSettings(nonExistentId, settingsToUpdate);
        
        // Assert
        assertFalse(result);
        verify(tenantService).findById(nonExistentId);
        verifyNoInteractions(settingsUtil);
        verify(tenantService, never()).updateTenant(any());
    }
    
    @Test
    void testApplyDefaultSettings() {
        // Arrange
        Tenant newTenant = new Tenant();
        newTenant.setName("New Tenant");
        newTenant.setTenantKey("new-tenant");
        newTenant.setSubdomain("new");
        newTenant.setBillingPlan("BASIC");
        newTenant.setDeleted(false);
        newTenant.setVersion(0);
        
        // Act
        Tenant result = tenantSettingsService.applyDefaultSettings(newTenant);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getSettings());
        assertFalse(result.getSettings().isEmpty());
        
        // The actual JSON content is tested via the TenantSettingsUtil tests
        // Here we just verify the settings were applied
        assertTrue(result.getSettings().contains("theme"));
        assertTrue(result.getSettings().contains("features"));
        assertTrue(result.getSettings().contains("email"));
    }
}