package dev.tiodati.saas.gocommerce.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.model.tenant.Tenant;
import dev.tiodati.saas.gocommerce.util.TenantSettingsUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.transaction.Transactional;

/**
 * Service for managing tenant settings and configurations.
 */
@ApplicationScoped
public class TenantSettingsService {
    
    @Inject
    TenantService tenantService;
    
    @Inject
    TenantSettingsUtil settingsUtil;
    
    /**
     * Get a specific tenant setting
     * 
     * @param tenantId The tenant ID
     * @param path The setting path (e.g., "theme.primaryColor")
     * @param defaultValue The default value if the setting is not found
     * @return The setting value, or default if not found
     */
    public String getSetting(UUID tenantId, String path, String defaultValue) {
        Optional<Tenant> tenant = tenantService.findById(tenantId);
        if (tenant.isEmpty()) {
            return defaultValue;
        }
        
        return settingsUtil.getSetting(tenant.get().getSettings(), path, defaultValue);
    }
    
    /**
     * Update a single tenant setting
     * 
     * @param tenantId The tenant ID
     * @param path The setting path (e.g., "theme.primaryColor")
     * @param value The new value
     * @return true if updated successfully, false otherwise
     */
    @Transactional
    public boolean updateSetting(UUID tenantId, String path, String value) {
        Optional<Tenant> optTenant = tenantService.findById(tenantId);
        if (optTenant.isEmpty()) {
            return false;
        }
        
        Tenant tenant = optTenant.get();
        String updatedSettings = settingsUtil.updateSetting(tenant.getSettings(), path, value);
        tenant.setSettings(updatedSettings);
        
        tenantService.updateTenant(tenant);
        return true;
    }
    
    /**
     * Update multiple tenant settings at once
     * 
     * @param tenantId The tenant ID
     * @param settings Map of setting paths to values
     * @return true if updated successfully, false otherwise
     */
    @Transactional
    public boolean updateSettings(UUID tenantId, Map<String, String> settings) {
        Optional<Tenant> optTenant = tenantService.findById(tenantId);
        if (optTenant.isEmpty()) {
            return false;
        }
        
        Tenant tenant = optTenant.get();
        String currentSettings = tenant.getSettings();
        
        // Update each setting
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            currentSettings = settingsUtil.updateSetting(currentSettings, entry.getKey(), entry.getValue());
        }
        
        tenant.setSettings(currentSettings);
        tenantService.updateTenant(tenant);
        return true;
    }
    
    /**
     * Set default tenant settings for a newly created tenant
     * 
     * @param tenant The tenant to update
     * @return The updated tenant with default settings
     */
    public Tenant applyDefaultSettings(Tenant tenant) {
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
            .add("maxProductsAllowed", tenant.getBillingPlan().equals("BASIC") ? 100 : 1000)
            .add("enableMultiCurrency", !tenant.getBillingPlan().equals("BASIC"));
        
        // Email settings
        JsonObjectBuilder email = Json.createObjectBuilder()
            .add("senderName", tenant.getName())
            .add("senderEmail", "noreply@" + tenant.getSubdomain() + ".gocommerce.example");
        
        // Payment settings
        JsonObjectBuilder payment = Json.createObjectBuilder()
            .add("providers", Json.createArrayBuilder().add("stripe"));
        
        // Combine all settings
        settings.add("theme", theme)
                .add("features", features)
                .add("email", email)
                .add("payment", payment);
        
        tenant.setSettings(settings.build().toString());
        return tenant;
    }
}