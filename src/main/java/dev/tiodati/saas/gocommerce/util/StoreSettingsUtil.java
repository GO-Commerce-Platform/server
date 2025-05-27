package dev.tiodati.saas.gocommerce.util;

import java.io.StringReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * Utility class for managing store settings.
 * Provides methods for working with JSON settings stored in the Store entity.
 */
@ApplicationScoped
public class StoreSettingsUtil {

    /**
     * Parse a JSON string into a JsonObjec
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
     * Get a specific setting value from store settings
     *
     * @param settings The JSON settings string
     * @param path The path to the setting, separated by dots (e.g., "theme.primaryColor")
     * @param defaultValue The default value to return if the path doesn't exis
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
            JsonValue value = current.get(key);
            switch (value.getValueType()) {
                case STRING:
                    return current.getString(key);
                case NUMBER:
                    return current.getJsonNumber(key).toString();
                case TRUE:
                    return "true";
                case FALSE:
                    return "false";
                default:
                    return value.toString();
            }
        }

        return defaultValue;
    }

    /**
     * Update or set a setting value in the store settings
     *
     * @param settings The current JSON settings string
     * @param path The path to the setting, separated by dots (e.g., "theme.primaryColor")
     * @param value The new value to se
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

        if (currentObject.containsKey(key) &&
            currentObject.get(key).getValueType() == JsonValue.ValueType.OBJECT) {
            // The path exists and is an object, we can continue
            nestedBuilder = Json.createObjectBuilder(currentObject.getJsonObject(key));
        } else {
            // The path doesn't exist or isn't an object, create a new one
            nestedBuilder = Json.createObjectBuilder();
        }

        // Recurse deeper
        updateSettingRecursive(nestedBuilder, parts, index + 1, value);

        // Update this level with the deeper objec
        builder.add(key, nestedBuilder);
    }

    /**
     * Check if a setting exists in the store settings
     *
     * @param settings The JSON settings string
     * @param path The path to the setting, separated by dots (e.g., "theme.primaryColor")
     * @return true if the setting exists, false otherwise
     */
    public boolean hasSetting(String settings, String path) {
        JsonObject jsonSettings = parseSettings(settings);
        String[] parts = path.split("\\.");

        JsonObject current = jsonSettings;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.containsKey(parts[i])) {
                return false;
            }

            JsonValue value = current.get(parts[i]);
            if (value == null || value.getValueType() != JsonValue.ValueType.OBJECT) {
                return false;
            }

            current = current.getJsonObject(parts[i]);
        }

        String key = parts[parts.length - 1];
        return current.containsKey(key);
    }

    /**
     * Remove a setting from the store settings
     *
     * @param settings The JSON settings string
     * @param path The path to the setting, separated by dots (e.g., "theme.primaryColor")
     * @return The updated settings JSON string
     */
    public String removeSetting(String settings, String path) {
        JsonObject jsonSettings = parseSettings(settings);
        String[] parts = path.split("\\.");

        JsonObjectBuilder rootBuilder = Json.createObjectBuilder(jsonSettings);
        removeSettingRecursive(rootBuilder, parts, 0);

        return rootBuilder.build().toString();
    }

    private boolean removeSettingRecursive(JsonObjectBuilder builder, String[] parts, int index) {
        if (index == parts.length - 1) {
            // We've reached the end of the path, remove the key
            JsonObject obj = builder.build();
            if (!obj.containsKey(parts[index])) {
                return false; // Key doesn't exist, nothing to remove
            }

            // Rebuild without the key
            JsonObjectBuilder newBuilder = Json.createObjectBuilder();
            obj.forEach((key, value) -> {
                if (!key.equals(parts[index])) {
                    newBuilder.add(key, value);
                }
            });

            // Replace the original builder with our new one that excludes the key
            obj.forEach((key, value) -> {
                if (!key.equals(parts[index])) {
                    builder.add(key, value);
                }
            });

            return true;
        }

        // We need to go deeper
        String key = parts[index];
        JsonObject currentObject = builder.build();

        if (!currentObject.containsKey(key) ||
            currentObject.get(key).getValueType() != JsonValue.ValueType.OBJECT) {
            return false; // Path doesn't exist, nothing to remove
        }

        JsonObjectBuilder nestedBuilder = Json.createObjectBuilder(currentObject.getJsonObject(key));
        boolean removed = removeSettingRecursive(nestedBuilder, parts, index + 1);

        if (removed) {
            // Update this level with the modified nested objec
            JsonObject nestedObject = nestedBuilder.build();
            if (nestedObject.isEmpty()) {
                // If the nested object is now empty, remove i
                // Rebuild without the key
                JsonObjectBuilder newBuilder = Json.createObjectBuilder();
                currentObject.forEach((k, v) -> {
                    if (!k.equals(key)) {
                        newBuilder.add(k, v);
                    }
                });

                // Replace the original builder with our new one that excludes the key
                currentObject.forEach((k, v) -> {
                    if (!k.equals(key)) {
                        builder.add(k, v);
                    }
                });
            } else {
                // Otherwise, update it with the modified nested objec
                builder.add(key, nestedBuilder);
            }
        }

        return removed;
    }
}
