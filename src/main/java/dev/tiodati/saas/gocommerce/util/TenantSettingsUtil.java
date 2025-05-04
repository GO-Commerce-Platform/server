package dev.tiodati.saas.gocommerce.util;

import java.io.StringReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * Utility class for managing tenant settings.
 * Provides methods for working with JSON settings stored in the Tenant entity.
 */
@ApplicationScoped
public class TenantSettingsUtil {

    /**
     * Parse a JSON string into a JsonObject
     * 
     * @param jsonString The JSON string to parse
     * @return A JsonObject representation of the string, or an empty JsonObject if parsing fails
     */
    public JsonObject parseSettings(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return Json.createObjectBuilder().build();
        }
        
        try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
            return reader.readObject();
        } catch (Exception e) {
            return Json.createObjectBuilder().build();
        }
    }
    
    /**
     * Get a specific setting value from tenant settings
     * 
     * @param settings The JSON settings string
     * @param path The path to the setting, separated by dots (e.g., "theme.primaryColor")
     * @param defaultValue The default value to return if the path doesn't exist
     * @return The setting value, or the default value if not found
     */
    public String getSetting(String settings, String path, String defaultValue) {
        JsonObject jsonSettings = parseSettings(settings);
        String[] parts = path.split("\\.");
        
        JsonObject current = jsonSettings;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.containsKey(parts[i])) {
                return defaultValue;
            }
            
            JsonValue value = current.get(parts[i]);
            if (value == null || value.getValueType() != JsonValue.ValueType.OBJECT) {
                return defaultValue;
            }
            
            current = current.getJsonObject(parts[i]);
        }
        
        String key = parts[parts.length - 1];
        if (current.containsKey(key)) {
            return current.getString(key, defaultValue);
        }
        
        return defaultValue;
    }
    
    /**
     * Update or set a setting value in the tenant settings
     * 
     * @param settings The current JSON settings string
     * @param path The path to the setting, separated by dots (e.g., "theme.primaryColor")
     * @param value The new value to set
     * @return The updated settings JSON string
     */
    public String updateSetting(String settings, String path, String value) {
        JsonObject jsonSettings = parseSettings(settings);
        String[] parts = path.split("\\.");
        
        JsonObjectBuilder rootBuilder = Json.createObjectBuilder(jsonSettings);
        updateSettingRecursive(rootBuilder, parts, 0, value);
        
        return rootBuilder.build().toString();
    }
    
    private void updateSettingRecursive(JsonObjectBuilder builder, String[] parts, int index, String value) {
        String key = parts[index];
        
        if (index == parts.length - 1) {
            // We've reached the end of the path, set the value
            builder.add(key, value);
            return;
        }
        
        // We need to go deeper
        JsonObjectBuilder nestedBuilder;
        JsonObject currentObject = builder.build();
        
        if (currentObject.containsKey(key)) {
            JsonValue jsonValue = currentObject.get(key);
            if (jsonValue != null && jsonValue.getValueType() == JsonValue.ValueType.OBJECT) {
                // Build upon existing object
                nestedBuilder = Json.createObjectBuilder(currentObject.getJsonObject(key));
            } else {
                // Replace non-object value with a new object
                nestedBuilder = Json.createObjectBuilder();
            }
        } else {
            // Create a new object if key doesn't exist
            nestedBuilder = Json.createObjectBuilder();
        }
        
        // Recurse to the next level
        updateSettingRecursive(nestedBuilder, parts, index + 1, value);
        
        // Add the nested object to this level
        builder.add(key, nestedBuilder.build());
    }
}