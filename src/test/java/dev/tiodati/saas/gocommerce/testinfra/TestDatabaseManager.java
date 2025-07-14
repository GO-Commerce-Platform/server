package dev.tiodati.saas.gocommerce.testinfra;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.tiodati.saas.gocommerce.platform.SchemaManager;
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

                // Dynamically get all tables in the schema to truncate them.
                // This is more robust than a hardcoded list of tables.
                var tablesRs = stmt.executeQuery(
                    "SELECT tablename FROM pg_tables WHERE schemaname = '" + schemaName + "'"
                );

                StringBuilder truncateCommand = new StringBuilder();
                while (tablesRs.next()) {
                    if (truncateCommand.length() > 0) {
                        truncateCommand.append(", ");
                    }
                    // Use double quotes for safety with table names
                    truncateCommand.append("\"").append(schemaName).append("\".\"").append(tablesRs.getString("tablename")).append("\"");
                }

                if (truncateCommand.length() > 0) {
                    // TRUNCATE is fast, and RESTART IDENTITY CASCADE handles foreign keys and resets sequences.
                    stmt.execute("TRUNCATE TABLE " + truncateCommand.toString() + " RESTART IDENTITY CASCADE");
                }
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
        // Use PreparedStatement to prevent SQL injection vulnerabilities.
        String sql = "SELECT 1 FROM information_schema.schemata WHERE schema_name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schemaName);
            var rs = ps.executeQuery();
            return rs.next();
        }
    }
}
