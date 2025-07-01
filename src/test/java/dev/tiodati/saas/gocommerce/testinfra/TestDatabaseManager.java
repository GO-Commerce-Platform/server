package dev.tiodati.saas.gocommerce.testinfra;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.tiodati.saas.gocommerce.store.SchemaManager;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Manages database schema lifecycle for tests in a multi-tenant environment.
 * Handles creation and cleanup of test store schemas to ensure test isolation.
 */
@ApplicationScoped
public class TestDatabaseManager {

    private final DataSource dataSource;
    private final SchemaManager schemaManager;
    
    @ConfigProperty(name = "gocommerce.test.default-store-schema", defaultValue = "test_store_default")
    private String defaultTestStoreSchema;
    
    private final Set<String> createdTestSchemas = ConcurrentHashMap.newKeySet();

    @Inject
    public TestDatabaseManager(DataSource dataSource, SchemaManager schemaManager) {
        this.dataSource = dataSource;
        this.schemaManager = schemaManager;
    }

    /**
     * Creates and migrates a test store schema if it doesn't exist.
     * Thread-safe and idempotent.
     *
     * @param schemaName the name of the schema to create
     */
    public void ensureTestStoreSchema(String schemaName) {
        if (createdTestSchemas.contains(schemaName)) {
            Log.debugf("Test schema already exists: %s", schemaName);
            return;
        }

        synchronized (this) {
            // Double-check locking pattern
            if (createdTestSchemas.contains(schemaName)) {
                return;
            }

            try {
                Log.infof("Creating test store schema: %s", schemaName);
                
                // Check if schema exists in database
                if (!schemaExists(schemaName)) {
                    // Create schema and run migrations
                    schemaManager.createSchema(schemaName);
                } else {
                    // Schema exists, just ensure migrations are up to date
                    schemaManager.migrateSchema(schemaName);
                }
                
                createdTestSchemas.add(schemaName);
                Log.infof("Test store schema ready: %s", schemaName);
                
            } catch (Exception e) {
                Log.errorf(e, "Failed to create test store schema: %s", schemaName);
                throw new RuntimeException("Failed to create test store schema: " + schemaName, e);
            }
        }
    }

    /**
     * Ensures the default test store schema is available.
     * This is useful for tests that don't need a specific store.
     */
    public void ensureDefaultTestStoreSchema() {
        ensureTestStoreSchema(defaultTestStoreSchema);
    }

    /**
     * Gets the name of the default test store schema.
     */
    public String getDefaultTestStoreSchema() {
        return defaultTestStoreSchema;
    }

    /**
     * Cleans all data from a test store schema while preserving the structure.
     * This is useful for test isolation without recreating the entire schema.
     *
     * @param schemaName the name of the schema to clean
     */
    public void cleanTestStoreSchema(String schemaName) {
        try {
            Log.debugf("Cleaning test store schema: %s", schemaName);
            
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Disable foreign key checks
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                
                // Clean data from all tables (order matters due to foreign keys)
                stmt.execute("DELETE FROM `" + schemaName + "`.cart_item");
                stmt.execute("DELETE FROM `" + schemaName + "`.shopping_cart");
                stmt.execute("DELETE FROM `" + schemaName + "`.order_items");
                stmt.execute("DELETE FROM `" + schemaName + "`.order_header");
                stmt.execute("DELETE FROM `" + schemaName + "`.product_review");
                stmt.execute("DELETE FROM `" + schemaName + "`.product_image");
                stmt.execute("DELETE FROM `" + schemaName + "`.product");
                stmt.execute("DELETE FROM `" + schemaName + "`.category");
                stmt.execute("DELETE FROM `" + schemaName + "`.customer");
                
                // Re-enable foreign key checks
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            
            Log.debugf("Test store schema cleaned: %s", schemaName);
            
        } catch (SQLException e) {
            Log.errorf(e, "Failed to clean test store schema: %s", schemaName);
            throw new RuntimeException("Failed to clean test store schema: " + schemaName, e);
        }
    }

    /**
     * Drops a test schema completely. Use with caution.
     *
     * @param schemaName the name of the schema to drop
     */
    public void dropTestStoreSchema(String schemaName) {
        try {
            Log.infof("Dropping test store schema: %s", schemaName);
            schemaManager.dropSchema(schemaName);
            createdTestSchemas.remove(schemaName);
            Log.infof("Test store schema dropped: %s", schemaName);
            
        } catch (SQLException e) {
            Log.errorf(e, "Failed to drop test store schema: %s", schemaName);
            throw new RuntimeException("Failed to drop test store schema: " + schemaName, e);
        }
    }

    /**
     * Cleans up all test schemas created during the test session.
     * This should be called in test teardown methods.
     */
    public void cleanupAllTestSchemas() {
        for (String schemaName : createdTestSchemas) {
            try {
                cleanTestStoreSchema(schemaName);
            } catch (Exception e) {
                Log.errorf(e, "Failed to cleanup test schema: %s", schemaName);
            }
        }
    }

    /**
     * Checks if a schema exists in the database.
     */
    private boolean schemaExists(String schemaName) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            var rs = stmt.executeQuery(
                "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + schemaName + "'"
            );
            return rs.next();
        }
    }
}
