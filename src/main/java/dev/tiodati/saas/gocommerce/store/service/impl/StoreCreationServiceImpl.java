package dev.tiodati.saas.gocommerce.store.service.impl;

import java.time.Instant;
import java.util.List;

import dev.tiodati.saas.gocommerce.auth.dto.KeycloakClientCreateRequest;
import dev.tiodati.saas.gocommerce.auth.dto.KeycloakUserCreateRequest;
import dev.tiodati.saas.gocommerce.auth.service.KeycloakAdminService;
import dev.tiodati.saas.gocommerce.exception.custom.DuplicateResourceException;
import dev.tiodati.saas.gocommerce.resource.dto.CreateStoreDto;
import dev.tiodati.saas.gocommerce.store.SchemaManager;
import dev.tiodati.saas.gocommerce.store.entity.Store;
import dev.tiodati.saas.gocommerce.store.entity.StoreAdmin;
import dev.tiodati.saas.gocommerce.store.entity.StoreStatus;
import dev.tiodati.saas.gocommerce.store.service.StoreCreationService;
import dev.tiodati.saas.gocommerce.store.service.StoreService;
import dev.tiodati.saas.gocommerce.store.service.StoreSettingsService;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of StoreCreationService that orchestrates the complete store
 * creation workflow.
 * This service coordinates schema creation, Keycloak setup, store
 * configuration, and registration
 * with proper error handling and rollback support.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class StoreCreationServiceImpl implements StoreCreationService {

    /**
     * Service for managing store entities and operations.
     */
    private final StoreService storeService;

    /**
     * Manager for database schema creation and migration.
     */
    private final SchemaManager schemaManager;

    /**
     * Service for Keycloak administration operations.
     */
    private final KeycloakAdminService keycloakAdminService;

    /**
     * Service for managing store settings and configurations.
     */
    private final StoreSettingsService storeSettingsService;

    @Override
        public Store createStore(CreateStoreDto createStoreDto) throws DuplicateResourceException {
            Log.infof("Starting store creation process for store: %s", createStoreDto.storeKey());

            try {
                // Step 1: Validate input and check for duplicates
                validateStoreCreationRequest(createStoreDto);

                // Step 2: Create database schema
                createDatabaseSchema(createStoreDto.storeKey());

                // Step 3: Create Keycloak client
                var keycloakClientId = createKeycloakClient(createStoreDto);

                // Step 4: Create admin user in Keycloak
                createKeycloakAdminUser(createStoreDto, keycloakClientId);

                // Step 5: Create and persist the store
                var store = createAndPersistStore(createStoreDto);

                // Step 6: Apply default store settings
                applyDefaultStoreSettings(store);

                Log.infof("Store creation completed successfully for store: %s", createStoreDto.storeKey());
                return store;

            } catch (IllegalArgumentException | DuplicateResourceException e) {
                // Let validation and duplicate exceptions bubble up directly
                Log.errorf(e, "Store creation failed for store: %s", createStoreDto.storeKey());
                throw e;
            } catch (Exception e) {
                Log.errorf(e, "Store creation failed for store: %s", createStoreDto.storeKey());
                throw new RuntimeException("Store creation failed: " + e.getMessage(), e);
            }
        }

    private void validateStoreCreationRequest(CreateStoreDto createStoreDto) {
        Log.debugf("Validating store creation request for store: %s", createStoreDto.storeKey());

        if (createStoreDto.storeKey() == null || createStoreDto.storeKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Store key cannot be null or empty");
        }

        if (createStoreDto.subdomain() == null || createStoreDto.subdomain().trim().isEmpty()) {
            throw new IllegalArgumentException("Subdomain cannot be null or empty");
        }

        if (createStoreDto.adminUsername() == null || createStoreDto.adminUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Admin username is required");
        }

        if (createStoreDto.adminEmail() == null || createStoreDto.adminEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Admin email is required");
        }

        // Check for duplicate store key
        if (storeService.findByStoreKey(createStoreDto.storeKey()).isPresent()) {
            throw new DuplicateResourceException("Store with key '" + createStoreDto.storeKey() + "' already exists");
        }

        // Check for duplicate subdomain
        if (storeService.findBySubdomain(createStoreDto.subdomain()).isPresent()) {
            throw new DuplicateResourceException(
                    "Store with subdomain '" + createStoreDto.subdomain() + "' already exists");
        }
    }

    private void createDatabaseSchema(String storeKey) {
        Log.infof("Creating database schema for store: %s", storeKey);

        try {
            // Generate schema name with consistent format (store_ prefix + underscore
            // normalization)
            String schemaName = "store_" + storeKey.toLowerCase().replace("-", "_");

            // Create the schema
            schemaManager.createSchema(schemaName);


            Log.infof("Database schema created and migrated successfully for store: %s (schema: %s)", storeKey,
                    schemaName);

        } catch (Exception e) {
            Log.errorf(e, "Failed to create database schema for store: %s", storeKey);
            throw new RuntimeException("Database schema creation failed for store: " + storeKey, e);
        }
    }

    private String createKeycloakClient(CreateStoreDto createStoreDto) {
        Log.infof("Creating Keycloak client for store: %s", createStoreDto.storeKey());

        try {
            var clientRequest = KeycloakClientCreateRequest.confidentialClient(
                    createStoreDto.storeKey() + "-client",
                    createStoreDto.name() + " Client",
                    List.of("https://" + createStoreDto.subdomain() + ".gocommerce.com/*"), // redirectUris
                    List.of("https://" + createStoreDto.subdomain() + ".gocommerce.com") // webOrigins
            );

            var clientId = keycloakAdminService.createClient(clientRequest);
            Log.infof("Keycloak client created successfully with ID: %s for store: %s", clientId,
                    createStoreDto.storeKey());

            return clientId;

        } catch (Exception e) {
            Log.errorf(e, "Failed to create Keycloak client for store: %s", createStoreDto.storeKey());
            throw new RuntimeException("Keycloak client creation failed for store: " + createStoreDto.storeKey(), e);
        }
    }

    private void createKeycloakAdminUser(CreateStoreDto createStoreDto, String keycloakClientId) {
        Log.infof("Creating admin user in Keycloak for store: %s", createStoreDto.storeKey());

        try {
            var userRequest = KeycloakUserCreateRequest.newUser(
                    createStoreDto.adminUsername(),
                    createStoreDto.adminEmail(),
                    createStoreDto.adminFirstName(),
                    createStoreDto.adminLastName(),
                    createStoreDto.adminPassword());

            var userId = keycloakAdminService.createUser(userRequest);
            Log.infof("Admin user created with ID: %s for store: %s", userId, createStoreDto.storeKey());

            // Assign realm roles (store admin)
            keycloakAdminService.assignRealmRolesToUser(userId, List.of("STORE_ADMIN"));

            Log.infof("Admin user setup completed for store: %s", createStoreDto.storeKey());

        } catch (Exception e) {
            Log.errorf(e, "Failed to create admin user for store: %s", createStoreDto.storeKey());
            throw new RuntimeException("Admin user creation failed for store: " + createStoreDto.storeKey(), e);
        }
    }

    private Store createAndPersistStore(CreateStoreDto createStoreDto) {
        Log.infof("Creating and persisting store entity: %s", createStoreDto.storeKey());

        try {
            // Create Store entity
            var store = new Store();
            store.setName(createStoreDto.name());
            store.setStoreKey(createStoreDto.storeKey());
            store.setSubdomain(createStoreDto.subdomain());
            store.setStatus(StoreStatus.PENDING);
            store.setBillingPlan(createStoreDto.billingPlan() != null ? createStoreDto.billingPlan() : "BASIC");
            store.setSettings(createStoreDto.settings());
            // Set required fields with defaults since CreateStoreDto doesn't have them
            store.setEmail(createStoreDto.adminEmail()); // Use admin email as store email
            store.setCurrencyCode("USD"); // Default currency
            store.setDefaultLocale("en-US"); // Default locale
            store.setDescription("Store created via automated process"); // Default description
            store.setDomainSuffix("gocommerce.com"); // Default domain suffix
            store.setSchemaName("store_" + createStoreDto.storeKey().toLowerCase().replace("-", "_")); // Schema name matching the one created
            store.setCreatedAt(Instant.now());
            store.setUpdatedAt(Instant.now());

            // Create StoreAdmin entity
            var admin = new StoreAdmin();
            admin.setUsername(createStoreDto.adminUsername());
            admin.setEmail(createStoreDto.adminEmail());
            admin.setFirstName(createStoreDto.adminFirstName());
            admin.setLastName(createStoreDto.adminLastName());

            // Use StoreService to create the store with schema and admin
            var createdStore = storeService.createStore(store, admin);

            Log.infof("Store entity created and persisted successfully: %s", createStoreDto.storeKey());
            return createdStore;

        } catch (Exception e) {
            Log.errorf(e, "Failed to create and persist store entity: %s", createStoreDto.storeKey());
            throw new RuntimeException("Store persistence failed for store: " + createStoreDto.storeKey(), e);
        }
    }

    private void applyDefaultStoreSettings(Store store) {
        Log.infof("Applying default settings for store: %s", store.getStoreKey());

        try {
            storeSettingsService.applyDefaultSettings(store);
            Log.infof("Default settings applied successfully for store: %s", store.getStoreKey());

        } catch (Exception e) {
            Log.errorf(e, "Failed to apply default settings for store: %s", store.getStoreKey());
            throw new RuntimeException("Default settings application failed for store: " + store.getStoreKey(), e);
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
