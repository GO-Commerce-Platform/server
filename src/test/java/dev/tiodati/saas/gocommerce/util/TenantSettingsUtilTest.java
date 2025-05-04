package dev.tiodati.saas.gocommerce.util;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TenantSettingsUtilTest {

    @Inject
    TenantSettingsUtil settingsUtil;
    
    @Test
    void testParseSettings_validJson() {
        // Arrange
        String jsonString = "{\"theme\":{\"primaryColor\":\"#FF5722\"},\"features\":{\"enableReviews\":true}}";
        
        // Act
        JsonObject result = settingsUtil.parseSettings(jsonString);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("theme"));
        assertTrue(result.getJsonObject("theme").containsKey("primaryColor"));
        assertEquals("#FF5722", result.getJsonObject("theme").getString("primaryColor"));
        assertTrue(result.getJsonObject("features").getBoolean("enableReviews"));
    }
    
    @Test
    void testParseSettings_emptyString() {
        // Arrange
        String jsonString = "";
        
        // Act
        JsonObject result = settingsUtil.parseSettings(jsonString);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    @Test
    void testParseSettings_nullString() {
        // Act
        JsonObject result = settingsUtil.parseSettings(null);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    @Test
    void testParseSettings_invalidJson() {
        // Arrange
        String jsonString = "{invalid json}";
        
        // Act
        JsonObject result = settingsUtil.parseSettings(jsonString);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    @Test
    void testGetSetting_existingPath() {
        // Arrange
        String jsonString = "{\"theme\":{\"primaryColor\":\"#FF5722\"},\"features\":{\"enableReviews\":true}}";
        
        // Act
        String result = settingsUtil.getSetting(jsonString, "theme.primaryColor", "default");
        
        // Assert
        assertEquals("#FF5722", result);
    }
    
    @Test
    void testGetSetting_nonExistentPath() {
        // Arrange
        String jsonString = "{\"theme\":{\"primaryColor\":\"#FF5722\"}}";
        
        // Act
        String result = settingsUtil.getSetting(jsonString, "theme.secondaryColor", "#000000");
        
        // Assert
        assertEquals("#000000", result);
    }
    
    @Test
    void testGetSetting_nonExistentParentPath() {
        // Arrange
        String jsonString = "{\"theme\":{\"primaryColor\":\"#FF5722\"}}";
        
        // Act
        String result = settingsUtil.getSetting(jsonString, "nonexistent.property", "default");
        
        // Assert
        assertEquals("default", result);
    }
    
    @Test
    void testUpdateSetting_newValue() {
        // Arrange
        String jsonString = "{\"theme\":{\"primaryColor\":\"#FF5722\"}}";
        
        // Act
        String updatedJson = settingsUtil.updateSetting(jsonString, "theme.primaryColor", "#2196F3");
        
        // Assert
        String expectedValue = settingsUtil.getSetting(updatedJson, "theme.primaryColor", "default");
        assertEquals("#2196F3", expectedValue);
    }
    
    @Test
    void testUpdateSetting_newPath() {
        // Arrange
        String jsonString = "{\"theme\":{\"primaryColor\":\"#FF5722\"}}";
        
        // Act
        String updatedJson = settingsUtil.updateSetting(jsonString, "theme.secondaryColor", "#2196F3");
        
        // Assert
        String expectedValue = settingsUtil.getSetting(updatedJson, "theme.secondaryColor", "default");
        assertEquals("#2196F3", expectedValue);
        
        // Original value should still be there
        String originalValue = settingsUtil.getSetting(updatedJson, "theme.primaryColor", "default");
        assertEquals("#FF5722", originalValue);
    }
    
    @Test
    void testUpdateSetting_newNestedPath() {
        // Arrange
        String jsonString = "{\"theme\":{\"primaryColor\":\"#FF5722\"}}";
        
        // Act
        String updatedJson = settingsUtil.updateSetting(jsonString, "features.enableReviews", "true");
        
        // Assert
        String expectedValue = settingsUtil.getSetting(updatedJson, "features.enableReviews", "false");
        assertEquals("true", expectedValue);
    }
    
    @Test
    void testUpdateSetting_emptyStartingJson() {
        // Arrange
        String jsonString = "";
        
        // Act
        String updatedJson = settingsUtil.updateSetting(jsonString, "theme.primaryColor", "#FF5722");
        
        // Assert
        String expectedValue = settingsUtil.getSetting(updatedJson, "theme.primaryColor", "default");
        assertEquals("#FF5722", expectedValue);
    }
}