package dev.tiodati.saas.gocommerce.store.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.store.model.Store;
import dev.tiodati.saas.gocommerce.util.StoreSettingsUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.transaction.Transactional;

/**
 * Service for managing store settings and configurations.
 */
@ApplicationScoped
public class StoreSettingsService {

    @Inject
    StoreService storeService;

    @Inject
    StoreSettingsUtil settingsUtil;

    /**
     * Get a specific store setting
     *
     * @param storeId      The store ID
     * @param path         The setting path (e.g., "theme.primaryColor")
     * @param defaultValue The default value if the setting is not found
     * @return The setting value, or default if not found
     */
    public String getSetting(UUID storeId, String path, String defaultValue) {
        Optional<Store> store = storeService.findById(storeId);
        if (store.isEmpty()) {
            return defaultValue;
        }

        return settingsUtil.getSetting(store.get().getSettings(), path, defaultValue);
    }

    /**
     * Update a single store setting
     *
     * @param storeId The store ID
     * @param path    The setting path (e.g., "theme.primaryColor")
     * @param value   The new value
     * @return true if updated successfully, false otherwise
     */
    @Transactional
    public boolean updateSetting(UUID storeId, String path, String value) {
        Optional<Store> optStore = storeService.findById(storeId);
        if (optStore.isEmpty()) {
            return false;
        }

        Store store = optStore.get();
        String updatedSettings = settingsUtil.updateSetting(store.getSettings(), path, value);
        store.setSettings(updatedSettings);

        storeService.updateStore(store);
        return true;
    }

    /**
     * Update multiple store settings at once
     *
     * @param storeId  The store ID
     * @param settings Map of setting paths to values
     * @return true if updated successfully, false otherwise
     */
    @Transactional
    public boolean updateSettings(UUID storeId, Map<String, String> settings) {
        Optional<Store> optStore = storeService.findById(storeId);
        if (optStore.isEmpty()) {
            return false;
        }

        Store store = optStore.get();
        String currentSettings = store.getSettings();

        // Update each setting
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            currentSettings = settingsUtil.updateSetting(currentSettings, entry.getKey(), entry.getValue());
        }

        store.setSettings(currentSettings);
        storeService.updateStore(store);
        return true;
    }

    /**
     * Set default store settings for a newly created store
     *
     * @param store The store to update
     * @return The updated store with default settings
     */
    public Store applyDefaultSettings(Store store) {
        JsonObjectBuilder settings = Json.createObjectBuilder();

        // Theme settings
        JsonObjectBuilder theme = Json.createObjectBuilder()
                .add("primaryColor", "#3498db")
                .add("secondaryColor", "#2ecc71")
                .add("logo", "")
                .add("favicon", "");

        // Feature flags
        JsonObjectBuilder features = Json.createObjectBuilder()
                .add("enableReviews", true)
                .add("enableWishlist", true)
                .add("maxProductsAllowed", store.getBillingPlan().equals("BASIC") ? 100 : 1000)
                .add("enableMultiCurrency", !store.getBillingPlan().equals("BASIC"));

        // Email settings
        JsonObjectBuilder email = Json.createObjectBuilder()
                .add("senderName", store.getName())
                .add("senderEmail", "noreply@" + store.getSubdomain() + ".gocommerce.example");

        // Payment settings
        JsonObjectBuilder payment = Json.createObjectBuilder()
                .add("providers", Json.createArrayBuilder().add("stripe"));

        // Combine all settings
        settings.add("theme", theme)
                .add("features", features)
                .add("email", email)
                .add("payment", payment);

        store.setSettings(settings.build().toString());
        return store;
    }
}
