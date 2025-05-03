package dev.tiodati.saas.gocommerce.tenant;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Manages database schema operations for multi-tenancy, including schema
 * creation, migration, and validation.
 */
@ApplicationScoped
public class SchemaManager {
    
    private static final Logger LOG = Logger.getLogger(SchemaManager.class);
    
    private final DataSource dataSource;
    
    @Inject
    public SchemaManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Creates a new database schema for a tenant
     * 
     * @param schemaName The schema name to create
     * @throws SQLException if schema creation fails
     */
    public void createSchema(String schemaName) throws SQLException {
        LOG.info("Creating schema: " + schemaName);
        
        try (Connection conn = dataSource.getConnection(); 
             Statement stmt = conn.createStatement()) {
            
            // Create schema if it doesn't exist
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
            LOG.info("Schema created successfully: " + schemaName);
            
            // Run migrations on new schema
            migrateSchema(schemaName);
        } catch (SQLException e) {
            LOG.error("Failed to create schema: " + schemaName, e);
            throw e;
        }
    }
    
    /**
     * Runs Flyway migrations on a specific tenant schema
     * 
     * @param schemaName The schema name to migrate
     */
    public void migrateSchema(String schemaName) {
        LOG.info("Running migrations on schema: " + schemaName);
        
        try {
            runFlywayMigration(schemaName);
            LOG.info("Migrations completed for schema: " + schemaName);
        } catch (Exception e) {
            LOG.error("Failed to run migrations on schema: " + schemaName, e);
            throw e;
        }
    }
    
    /**
     * Protected method that runs the actual Flyway migration.
     * This is extracted to make the class more testable.
     * 
     * @param schemaName The schema name to migrate
     */
    protected void runFlywayMigration(String schemaName) {
        FluentConfiguration flywayConfig = Flyway.configure()
            .dataSource(dataSource)
            .schemas(schemaName)
            .locations("db/migration/tenants")
            .baselineOnMigrate(true)
            .validateOnMigrate(true);
            
        Flyway flyway = flywayConfig.load();
        flyway.migrate();
    }
    
    /**
     * Drops a schema and all its objects (use with caution!)
     * 
     * @param schemaName The schema name to drop
     * @throws SQLException if schema deletion fails
     */
    public void dropSchema(String schemaName) throws SQLException {
        LOG.warn("Dropping schema: " + schemaName);
        
        try (Connection conn = dataSource.getConnection(); 
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
            LOG.info("Schema dropped: " + schemaName);
        } catch (SQLException e) {
            LOG.error("Failed to drop schema: " + schemaName, e);
            throw e;
        }
    }
}