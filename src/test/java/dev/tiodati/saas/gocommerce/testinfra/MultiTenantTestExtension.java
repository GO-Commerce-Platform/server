package dev.tiodati.saas.gocommerce.testinfra;

import java.util.UUID;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import dev.tiodati.saas.gocommerce.testinfra.TestTenantResolver;
import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.logging.Log;

/**
 * JUnit 5 extension that automatically sets up multi-tenant database schemas for tests.
 * This extension ensures that store-specific schemas are created and configured before tests run.
 */
public class MultiTenantTestExtension implements BeforeAllCallback, BeforeEachCallback, TestInstancePostProcessor {

    private static final String TEST_SCHEMA_KEY = "test.schema.name";
    private static TestDatabaseManager testDatabaseManager;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Log.infof("Multi-tenant test environment initialized for test class: %s", 
                  context.getTestClass().map(Class::getSimpleName).orElse("Unknown"));
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Initialize the test database manager if not already done
        if (testDatabaseManager == null && Arc.container() != null) {
            testDatabaseManager = Arc.container().instance(TestDatabaseManager.class).get();
        }

        // Get or create a test schema for this test
        String testSchemaName = getTestSchemaName(context);
        
        // Ensure the test schema exists
        if (testDatabaseManager != null) {
            testDatabaseManager.ensureTestStoreSchema(testSchemaName);
        }
        
        // Set the tenant context for this test
        setTenantContext(testSchemaName);
        
        Log.debugf("Test schema set up for test: %s -> %s", 
                   context.getDisplayName(), testSchemaName);
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        // If the test instance has a schema field, inject it
        try {
            var schemaField = testInstance.getClass().getDeclaredField("testSchemaName");
            schemaField.setAccessible(true);
            schemaField.set(testInstance, getTestSchemaName(context));
        } catch (NoSuchFieldException e) {
            // Field doesn't exist, which is fine - not all tests need it
        }
    }

    /**
     * Gets the test schema name for a specific test context.
     * Uses the test method name to create a unique schema per test.
     */
    private String getTestSchemaName(ExtensionContext context) {
        // Check if a custom schema name is stored in the context
        String customSchema = context.getStore(ExtensionContext.Namespace.GLOBAL)
                                    .get(TEST_SCHEMA_KEY, String.class);
        if (customSchema != null) {
            return customSchema;
        }

        // Generate a schema name based on test class and method
        String className = context.getTestClass()
                                 .map(Class::getSimpleName)
                                 .orElse("Test");
        String methodName = context.getTestMethod()
                                  .map(method -> method.getName())
                                  .orElse("unknown");

        // Create a unique but deterministic schema name
        String schemaName = String.format("test_%s_%s_%s", 
                                         className.toLowerCase(),
                                         methodName.toLowerCase(),
                                         UUID.randomUUID().toString().substring(0, 8))
                                  .replaceAll("[^a-zA-Z0-9_]", "_");

        // Store it for potential reuse
        context.getStore(ExtensionContext.Namespace.GLOBAL)
               .put(TEST_SCHEMA_KEY, schemaName);

        return schemaName;
    }

    /**
     * Sets the tenant context for Hibernate multi-tenancy.
     */
    private void setTenantContext(String schemaName) {
        // Set the tenant identifier for Hibernate using TestTenantResolver
        try {
            Arc.container().instance(TestTenantContext.class).get().setCurrentTenant(schemaName);
            Log.debugf("Set tenant context to: %s", schemaName);
        } catch (Exception e) {
            Log.warnf("Could not set tenant context: %s", e.getMessage());
        }
    }

    /**
     * Utility method to set a custom schema name for a test.
     * Call this in @BeforeEach if you need a specific schema name.
     */
    public static void setCustomTestSchema(ExtensionContext context, String schemaName) {
        context.getStore(ExtensionContext.Namespace.GLOBAL)
               .put(TEST_SCHEMA_KEY, schemaName);
    }

    /**
     * Utility method to get the current test schema name.
     */
    public static String getCurrentTestSchema(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.GLOBAL)
                     .get(TEST_SCHEMA_KEY, String.class);
    }
}