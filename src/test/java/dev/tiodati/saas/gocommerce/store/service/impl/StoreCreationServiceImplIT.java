package dev.tiodati.saas.gocommerce.store.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import dev.tiodati.saas.gocommerce.exception.custom.DuplicateResourceException;
import dev.tiodati.saas.gocommerce.resource.dto.CreateStoreDto;
import dev.tiodati.saas.gocommerce.store.entity.Store;
import dev.tiodati.saas.gocommerce.store.entity.StoreStatus;
import dev.tiodati.saas.gocommerce.store.service.StoreCreationService;
import dev.tiodati.saas.gocommerce.store.service.StoreService;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;

/**
 * Integration test for StoreCreationService that validates the complete store
 * creation workflow
 * including database schema creation, Keycloak client/user setup, and store
 * persistence.
 *
 * This test validates that our configuration properties for store creation work
 * correctly:
 * - Database schema admin credentials for multi-tenant schema creation
 * - Keycloak Admin Client configuration for service-to-service communication
 * - Complete workflow orchestration with proper error handling
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StoreCreationServiceImplIT {

    /** The store creation service under test. */
    @Inject
    private StoreCreationService storeCreationService;

    /** Store service for verification operations. */
    @Inject
    private StoreService storeService;

    /** Database connection for schema verification. */
    @Inject
    private DataSource dataSource;

    /** Keycloak admin client for verification operations. */
    @Inject
    private Keycloak keycloakAdminClient;

    /** Test store key for integration tests. */
    private String testStoreKey;

    /** Test subdomain for integration tests. */
    private String testSubdomain;

    /** Expected database schema name. */
    private String expectedSchemaName;

    /** Test Keycloak client ID. */
    private String testClientId;

    /** Test admin username. */
    private String testAdminUsername;

    /** Test store creation DTO. */
    private CreateStoreDto testStoreDto;

    @BeforeEach
    void setUp() {
        // Generate unique identifiers for each test
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        testStoreKey = "test-store-" + uniqueSuffix;
        testSubdomain = "test-sub-" + uniqueSuffix;
        expectedSchemaName = "store_" + testStoreKey.replace("-", "_");
        testClientId = testStoreKey + "-client";
        testAdminUsername = "admin-" + uniqueSuffix;

        // Create test store DTO using record constructor
        testStoreDto = new CreateStoreDto(
                testStoreKey,
                "Test Store " + uniqueSuffix,
                testSubdomain,
                "BASIC",
                "{}",
                testAdminUsername,
                testAdminUsername + "@test.com",
                "testpass123",
                "Test",
                "Admin");

        Log.infof("Set up test with storeKey=%s, subdomain=%s, schema=%s",
                testStoreKey, testSubdomain, expectedSchemaName);
    }

    @AfterEach
    void tearDown() {
        try {
            // Clean up database schema
            cleanupDatabaseSchema();

            // Clean up Keycloak resources
            cleanupKeycloakResources();

            // Clean up store entity
            cleanupStoreEntity();

        } catch (Exception e) {
            Log.warnf(e, "Failed to clean up test resources for store: %s", testStoreKey);
        }
    }

    @Test
    @Order(1)
    @Transactional
    void testCompleteStoreCreationWorkflow() throws Exception {
        Log.info("Testing complete store creation workflow");

        // When - Create the store
        Store createdStore = storeCreationService.createStore(testStoreDto);

        // Then - Verify store entity was created
        assertNotNull(createdStore);
        assertNotNull(createdStore.getId());
        assertEquals(testStoreDto.name(), createdStore.getName());
        assertEquals(testStoreDto.storeKey(), createdStore.getStoreKey());
        assertEquals(testStoreDto.subdomain(), createdStore.getSubdomain());
        assertEquals(StoreStatus.PENDING, createdStore.getStatus());
        assertNotNull(createdStore.getCreatedAt());

        // Verify database schema was created
        assertTrue(schemaExists(expectedSchemaName),
                "Database schema should exist: " + expectedSchemaName);

        // Verify schema contains migrated tables
        assertTrue(tableExists(expectedSchemaName, "flyway_schema_history"),
                "Flyway history table should exist");
        assertTrue(tableExists(expectedSchemaName, "customer"),
                "Customer table should exist after migration");
        assertTrue(tableExists(expectedSchemaName, "product"),
                "Product table should exist after migration");

        // Verify Keycloak client was created
        assertTrue(keycloakClientExists(testClientId),
                "Keycloak client should exist: " + testClientId);

        // Verify Keycloak admin user was created
        assertTrue(keycloakUserExists(testAdminUsername),
                "Keycloak admin user should exist: " + testAdminUsername);

        Log.infof("Complete store creation workflow validated successfully for store: %s", testStoreKey);
    }

    @Test
    @Order(2)
    @Transactional
    void testDuplicateStoreKeyValidation() {
        Log.info("Testing duplicate store key validation");

        // Given - Create first store
        Store firstStore = storeCreationService.createStore(testStoreDto);
        assertNotNull(firstStore);

        // When/Then - Attempt to create store with same key
        CreateStoreDto duplicateDto = new CreateStoreDto(
                testStoreKey, // Same key
                "Duplicate Store",
                "different-subdomain-" + UUID.randomUUID().toString().substring(0, 8),
                "BASIC",
                "{}",
                "different-admin-" + UUID.randomUUID().toString().substring(0, 8),
                "different@test.com",
                "testpass123",
                "Different",
                "Admin");

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> storeCreationService.createStore(duplicateDto));

        assertTrue(exception.getMessage().contains("Store with key"));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @Order(3)
    @Transactional
    void testDuplicateSubdomainValidation() {
        Log.info("Testing duplicate subdomain validation");

        // Given - Create first store
        Store firstStore = storeCreationService.createStore(testStoreDto);
        assertNotNull(firstStore);

        // When/Then - Attempt to create store with same subdomain
        CreateStoreDto duplicateDto = new CreateStoreDto(
                "different-key-" + UUID.randomUUID().toString().substring(0, 8),
                "Duplicate Subdomain Store",
                testSubdomain, // Same subdomain
                "BASIC",
                "{}",
                "different-admin-" + UUID.randomUUID().toString().substring(0, 8),
                "different@test.com",
                "testpass123",
                "Different",
                "Admin");

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> storeCreationService.createStore(duplicateDto));

        assertTrue(exception.getMessage().contains("Store with subdomain"));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @Order(4)
    @Transactional
    void testInvalidInputValidation() {
        Log.info("Testing invalid input validation");

        // Test null store key
        CreateStoreDto invalidDto1 = new CreateStoreDto(
                null, // Invalid
                "Test Store",
                "test-subdomain",
                "BASIC",
                "{}",
                "admin",
                "admin@test.com",
                "testpass123",
                "Test",
                "Admin");

        assertThrows(IllegalArgumentException.class,
                () -> storeCreationService.createStore(invalidDto1));

        // Test empty subdomain
        CreateStoreDto invalidDto2 = new CreateStoreDto(
                "test-store",
                "Test Store",
                "", // Invalid
                "BASIC",
                "{}",
                "admin",
                "admin@test.com",
                "testpass123",
                "Test",
                "Admin");

        assertThrows(IllegalArgumentException.class,
                () -> storeCreationService.createStore(invalidDto2));
    }

    // Helper methods for validation

    private boolean schemaExists(String schemaName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + schemaName
                                + "'")) {
            return resultSet.next();
        }
    }

    private boolean tableExists(String schemaName, String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '"
                                + schemaName + "' AND TABLE_NAME = '" + tableName + "'")) {
            return resultSet.next();
        }
    }

    private boolean keycloakClientExists(String clientId) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm("gocommerce");
            List<ClientRepresentation> clients = realmResource.clients().findByClientId(clientId);
            return !clients.isEmpty();
        } catch (Exception e) {
            Log.warnf(e, "Failed to check if Keycloak client exists: %s", clientId);
            return false;
        }
    }

    private boolean keycloakUserExists(String username) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm("gocommerce");
            UsersResource usersResource = realmResource.users();
            List<UserRepresentation> users = usersResource.search(username, true);
            return users.stream().anyMatch(user -> username.equals(user.getUsername()));
        } catch (Exception e) {
            Log.warnf(e, "Failed to check if Keycloak user exists: %s", username);
            return false;
        }
    }

    // Cleanup methods

    private void cleanupDatabaseSchema() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            if (schemaExists(expectedSchemaName)) {
                statement.executeUpdate("DROP SCHEMA IF EXISTS `" + expectedSchemaName + "`");
                Log.infof("Cleaned up database schema: %s", expectedSchemaName);
            }
        } catch (Exception e) {
            Log.warnf(e, "Failed to clean up database schema: %s", expectedSchemaName);
        }
    }

    private void cleanupKeycloakResources() {
        try {
            RealmResource realmResource = keycloakAdminClient.realm("gocommerce");

            // Clean up client
            List<ClientRepresentation> clients = realmResource.clients().findByClientId(testClientId);
            for (ClientRepresentation client : clients) {
                realmResource.clients().get(client.getId()).remove();
                Log.infof("Cleaned up Keycloak client: %s", testClientId);
            }

            // Clean up user
            UsersResource usersResource = realmResource.users();
            List<UserRepresentation> users = usersResource.search(testAdminUsername, true);
            for (UserRepresentation user : users) {
                if (testAdminUsername.equals(user.getUsername())) {
                    usersResource.get(user.getId()).remove();
                    Log.infof("Cleaned up Keycloak user: %s", testAdminUsername);
                }
            }
        } catch (Exception e) {
            Log.warnf(e, "Failed to clean up Keycloak resources for store: %s", testStoreKey);
        }
    }

    private void cleanupStoreEntity() {
        try {
            storeService.findByStoreKey(testStoreKey).ifPresent(store -> {
                // Note: In a real scenario, you might want to use a proper deletion method
                // This is just for test cleanup
                Log.infof("Store entity exists and should be cleaned up: %s", testStoreKey);
            });
        } catch (Exception e) {
            Log.warnf(e, "Failed to clean up store entity: %s", testStoreKey);
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
