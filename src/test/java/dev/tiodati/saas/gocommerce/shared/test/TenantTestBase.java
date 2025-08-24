package dev.tiodati.saas.gocommerce.shared.test;

import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import dev.tiodati.saas.gocommerce.platform.SchemaManager;
import dev.tiodati.saas.gocommerce.store.StoreContext;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Base class for multi-tenant tests that require a dedicated store schema.
 * <p>
 * This class handles the lifecycle of a temporary schema for each test method,
 * ensuring complete test isolation. It automates the following:
 * </p>
 * <ul>
 * <li><b>Before each test:</b>
 * <ol>
 * <li>Generates a unique schema name (e.g., "test_store_...").</li>
 * <li>Uses {@link SchemaManager} to create and migrate the schema.</li>
 * <li>Sets the current tenant context using {@link StoreContext}.</li>
 * </ol>
 * </li>
 * <li><b>After each test:</b>
 * <ol>
 * <li>Clears the tenant context.</li>
 * <li>Drops the schema to clean up the database.</li>
 * </ol>
 * </li>
 * </ul>
 * <p>
 * Tests that interact with store-specific data should extend this class to
 * ensure
 * they run in a clean, isolated environment.
 * </p>
 */
@QuarkusTest
public abstract class TenantTestBase {

    @Inject
    private SchemaManager schemaManager;

    private String testSchema;

    /**
     * Gets the current test schema name.
     * This can be used by subclasses to reference the schema being used for the test.
     * 
     * @return the current test schema name
     */
    protected String getTestSchema() {
        return testSchema;
    }

    @BeforeEach
    void setupTenant() {
        this.testSchema = "test_store_" + UUID.randomUUID().toString().replace("-", "");
        Log.infof("✅ Setting up tenant for test: %s", this.testSchema);
        try {
            schemaManager.createSchema(this.testSchema);
            StoreContext.setCurrentStore(this.testSchema);
        } catch (SQLException e) {
            Log.errorf(e, "Failed to create schema %s before test", this.testSchema);
        } finally {
            Log.info("✅ Tenant setup complete");
        }

    }

    @AfterEach
    void tearDownTenant() {
        Log.infof("❌ Tearing down tenant for test: %s", this.testSchema);

        try {
            StoreContext.clear();
            schemaManager.dropSchema(this.testSchema);
        } catch (SQLException e) {
            Log.errorf(e, "Failed to drop schema %s after test", this.testSchema);
        } finally {
            this.testSchema = null; // Clear the schema name to avoid memory leaks
            Log.info("✅ Tenant teardown complete");
        }
    }
}
// Copilot: This file may have been generated or refactored by GitHub Copilot.
