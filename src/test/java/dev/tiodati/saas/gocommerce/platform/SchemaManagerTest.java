package dev.tiodati.saas.gocommerce.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration test for SchemaManager that validates multi-schema Flyway
 * functionality against the PostgreSQL database stack.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SchemaManagerTest {

    /** The schema manager under test. */
    @Inject
    private SchemaManager schemaManager;

    /** The data source for database operations. */
    @Inject
    private DataSource dataSource;

    /** Test store ID for generating unique schema names. */
    private static final String TEST_STORE_ID = "test-store-"
            + UUID.randomUUID().toString().substring(0, 8);

    /** Expected schema name based on the test store ID. */
    private static final String EXPECTED_SCHEMA_NAME = "store_"
            + TEST_STORE_ID.replace("-", "_");

    @Test
    @Order(1)
    void testCreateStoreSchemaIntegration() throws Exception {
        // When - Use createSchema method which creates schema and runs
        // migrations
        schemaManager.createSchema(EXPECTED_SCHEMA_NAME);

        // Then - Verify schema was created
        assertTrue(schemaExists(EXPECTED_SCHEMA_NAME),
                "Schema should exist after creation: " + EXPECTED_SCHEMA_NAME);
    }

    @Test
    @Order(2)
    void testSchemaContainsFlywayMigrationTable() throws Exception {
        // Given - Schema should already exist from previous test
        assertTrue(schemaExists(EXPECTED_SCHEMA_NAME),
                "Test schema should exist");

        // Then - Verify Flyway migration table exists in the schema
        assertTrue(tableExists(EXPECTED_SCHEMA_NAME, "flyway_schema_history"),
                "Flyway migration table should exist in schema: "
                        + EXPECTED_SCHEMA_NAME);
    }

    @Test
    @Order(3)
    void testSchemaContainsMigratedTables() throws Exception {
        // Given - Schema should already exist with migrations run
        assertTrue(schemaExists(EXPECTED_SCHEMA_NAME),
                "Test schema should exist");

        // Then - Verify core e-commerce tables exist
        assertTrue(tableExists(EXPECTED_SCHEMA_NAME, "customer"),
                "Customer table should exist after migration");
        assertTrue(tableExists(EXPECTED_SCHEMA_NAME, "category"),
                "Category table should exist after migration");
        assertTrue(tableExists(EXPECTED_SCHEMA_NAME, "product"),
                "Product table should exist after migration");
        assertTrue(tableExists(EXPECTED_SCHEMA_NAME, "order_header"),
                "Order header table should exist after migration");
        assertTrue(tableExists(EXPECTED_SCHEMA_NAME, "order_items"),
                "Order items table should exist after migration");
        assertTrue(tableExists(EXPECTED_SCHEMA_NAME, "shopping_cart"),
                "Shopping cart table should exist after migration");
        assertTrue(tableExists(EXPECTED_SCHEMA_NAME, "cart_item"),
                "Cart item table should exist after migration");
        assertTrue(tableExists(EXPECTED_SCHEMA_NAME, "product_image"),
                "Product image table should exist after migration");
        assertTrue(tableExists(EXPECTED_SCHEMA_NAME, "product_review"),
                "Product review table should exist after migration");
    }

    @Test
    @Order(4)
    void testFlywayMigrationVersionsRecorded() throws Exception {
        // Given
        assertTrue(schemaExists(EXPECTED_SCHEMA_NAME),
                "Test schema should exist");

        // When - Query migration history
        int migrationCount = getMigrationCount(EXPECTED_SCHEMA_NAME);

        // Then - Should have 6 migrations (V3, V4, V5, V6, V8, V9, V10)
        assertEquals(7, migrationCount,
                "Should have 6 migration records in Flyway history table");
    }

    @Test
    @Order(5)
    void testCreateSchemaDirectly() throws Exception {
        // Given
        String directSchemaName = "direct_test_"
                + UUID.randomUUID().toString().substring(0, 8);

        // When
        schemaManager.createSchema(directSchemaName);

        // Then
        assertTrue(schemaExists(directSchemaName),
                "Directly created schema should exist: " + directSchemaName);

        // Cleanup
        dropSchema(directSchemaName);
    }

    @Test
    @Order(6)
    void testRunFlywayMigrationOnExistingSchema() throws Exception {
        // Given - Create a new schema without migrations
        String migrationTestSchema = "migration_test_"
                + UUID.randomUUID().toString().substring(0, 8);

        // Create the schema manually without migrations
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE SCHEMA IF NOT EXISTS \"" + migrationTestSchema + "\"");
        }

        assertTrue(schemaExists(migrationTestSchema),
                "Test schema should exist");

        // When - Run Flyway migration
        schemaManager.migrateSchema(migrationTestSchema);

        // Then - Verify migration table and core tables exist
        assertTrue(tableExists(migrationTestSchema, "flyway_schema_history"),
                "Flyway table should exist after migration");
        assertTrue(tableExists(migrationTestSchema, "customer"),
                "Customer table should exist after migration");

        // Cleanup
        dropSchema(migrationTestSchema);
    }

    /**
     * Helper method to check if a schema exists in the database.
     *
     * @param schemaName the name of the schema to check
     * @return true if the schema exists, false otherwise
     * @throws SQLException if database query fails
     */
    private boolean schemaExists(String schemaName) throws SQLException {
        final String sql = "SELECT 1 FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?";
        try (Connection connection = dataSource.getConnection();
             java.sql.PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schemaName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Helper method to check if a table exists in a specific schema.
     *
     * @param schemaName the name of the schema
     * @param tableName  the name of the table to check
     * @return true if the table exists in the schema, false otherwise
     * @throws SQLException if database query fails
     */
    private boolean tableExists(String schemaName, String tableName)
            throws SQLException {
        final String sql = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (Connection connection = dataSource.getConnection();
             java.sql.PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schemaName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Helper method to get the count of applied migrations in a schema.
     *
     * @param schemaName the name of the schema
     * @return the number of successful migrations
     * @throws SQLException if database query fails
     */
    private int getMigrationCount(String schemaName) throws SQLException {
        // Schema name cannot be parameterized, so it's validated by context.
        final String sql = "SELECT COUNT(*) FROM " + schemaName + ".flyway_schema_history WHERE success = ?";
        try (Connection connection = dataSource.getConnection();
                java.sql.PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, true);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Helper method to drop a schema for cleanup.
     *
     * @param schemaName the name of the schema to drop
     * @throws SQLException if schema deletion fails
     */
    private void dropSchema(String schemaName) throws SQLException {
        // Use the main SchemaManager to ensure consistent cleanup logic
        // (e.g., using CASCADE)
        schemaManager.dropSchema(schemaName);
    }
}
