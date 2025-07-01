package dev.tiodati.saas.gocommerce.store;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import io.quarkus.logging.Log;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Manages database schema operations for multi-tenancy, including schema
 * creation, migration, and validation.
 */
@ApplicationScoped
public class SchemaManager {

    /** The data source for database operations. */
    private final DataSource dataSource;

    /** The location of store-specific migration files. */
    @ConfigProperty(name = "gocommerce.flyway.store-migrations.locations", defaultValue = "db/migration/stores")
    private String storeMigrationsLocation;

    /** The name of the Flyway schema history table for store migrations. */
    @ConfigProperty(name = "gocommerce.flyway.store-migrations.table", defaultValue = "flyway_schema_history")
    private String storeMigrationsTable;

    /** The baseline version for new store schema migrations. */
    @ConfigProperty(name = "gocommerce.flyway.store-migrations.baseline-version", defaultValue = "1.0.0")
    private String storeBaselineVersion;

    /** Whether to validate migrations on migrate for store schemas. */
    @ConfigProperty(name = "gocommerce.flyway.store-migrations.validate-on-migrate", defaultValue = "true")
    private boolean storeValidateOnMigrate;
    
    /** The database URL for creating separate datasource connections. */
    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    private String datasourceUrl;
    
    /** The database username. */
    @ConfigProperty(name = "quarkus.datasource.username")
    private String datasourceUsername;
    
    /** The database password. */
    @ConfigProperty(name = "quarkus.datasource.password")
    private String datasourcePassword;

    /**
     * Constructor for dependency injection.
     *
     * @param dataSource the data source to use for schema operations
     */
    @Inject
    public SchemaManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Creates a new database schema for a store.
     *
     * @param schemaName The schema name to create
     * @throws SQLException if schema creation fails
     */
    public void createSchema(String schemaName) throws SQLException {
        Log.info("Creating schema: " + schemaName);

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            // Create schema if it doesn't exist
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
            Log.info("Schema created successfully: " + schemaName);

        } catch (SQLException e) {
            Log.error("Failed to create schema: " + schemaName, e);
            throw e;
        }
        
        // Run migrations on new schema outside of transaction
        migrateSchema(schemaName);
    }

    /**
     * Runs Flyway migrations on a specific store schema.
     *
     * @param schemaName The schema name to migrate
     */
    public void migrateSchema(String schemaName) {
        Log.info("Running migrations on schema: " + schemaName);

        try {
            runFlywayMigration(schemaName);
            Log.info("Migrations completed for schema: " + schemaName);
        } catch (Exception e) {
            Log.error("Failed to run migrations on schema: " + schemaName, e);
            throw e;
        }
    }

    /**
     * Protected method that runs the actual Flyway migration. This is extracted
     * to make the class more testable.
     *
     * @param schemaName The schema name to migrate
     */
    protected void runFlywayMigration(String schemaName) {
        // Use URL-based datasource configuration for Flyway to avoid JTA transaction conflicts
        FluentConfiguration flywayConfig = Flyway.configure()
                .dataSource(datasourceUrl, datasourceUsername, datasourcePassword)
                .schemas(schemaName)
                .locations(storeMigrationsLocation)
                .table(storeMigrationsTable)
                .baselineVersion(storeBaselineVersion)
                .baselineOnMigrate(true)
                .validateOnMigrate(storeValidateOnMigrate)
                .cleanDisabled(true); // Safety: prevent accidental clean operations

        Flyway flyway = flywayConfig.load();
        flyway.migrate();
    }

    /**
     * Drops a schema and all its objects (use with caution!).
     *
     * @param schemaName The schema name to drop
     * @throws SQLException if schema deletion fails
     */
    public void dropSchema(String schemaName) throws SQLException {
        Log.warn("Dropping schema: " + schemaName);

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
            Log.info("Schema dropped: " + schemaName);
        } catch (SQLException e) {
            Log.error("Failed to drop schema: " + schemaName, e);
            throw e;
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
