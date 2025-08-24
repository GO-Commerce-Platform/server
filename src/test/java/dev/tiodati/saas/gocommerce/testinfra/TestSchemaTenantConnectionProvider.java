package dev.tiodati.saas.gocommerce.testinfra;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Unremovable;
import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;

/**
 * Test-specific multi-tenant connection provider for schema-based tenancy.
 * This provider switches the database schema based on the tenant ID and has
 * higher priority than the production provider.
 */
// TEMPORARILY DISABLED - Testing if Quarkus default SCHEMA provider works
// @ApplicationScoped
// @Alternative
// @Priority(1) // Higher priority than production provider
// @Unremovable
public class TestSchemaTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    @Inject
    private AgroalDataSource dataSource;

    @Inject
    private TestTenantContext testTenantContext;
    
    @PostConstruct
    public void init() {
        Log.infof("🎉 TEST MTCP: TestSchemaTenantConnectionProvider initialized! Multi-tenancy should be working.");
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        Log.infof("📞 TEST MTCP: getAnyConnection called");
        Connection conn = dataSource.getConnection();
        Log.infof("📞 TEST MTCP: getAnyConnection returning: %s", conn.getClass().getSimpleName());
        return conn;
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Log.infof("🔥 TEST MTCP: *** getConnection() CALLED *** with tenant: %s", tenantIdentifier);
        
        // Use TestTenantContext to get the actual tenant if available
        String actualTenantId = tenantIdentifier;
        if (testTenantContext != null && testTenantContext.getCurrentTenant() != null) {
            actualTenantId = testTenantContext.getCurrentTenant();
            Log.infof("TEST MTCP: Using tenant from TestTenantContext: %s (requested: %s)", actualTenantId, tenantIdentifier);
        } else {
            Log.infof("TEST MTCP: Using requested tenant: %s (TestTenantContext not available or empty)", tenantIdentifier);
        }
        
        Connection connection = getAnyConnection();
        Log.infof("🔗 TEST MTCP: Got connection: %s", connection.getClass().getSimpleName());
        
        try {
            // Switch to the tenant's schema
            if (actualTenantId != null && !actualTenantId.trim().isEmpty()) {
                Log.infof("🔄 TEST: Switching database schema to: %s", actualTenantId);
                try (Statement statement = connection.createStatement()) {
                    // Use quoted identifier to handle special characters in schema names
                    String sql = String.format("SET search_path TO \"%s\", public", actualTenantId);
                    Log.infof("📝 TEST: Executing SQL: %s", sql);
                    statement.execute(sql);
                    Log.infof("✅ TEST: Successfully executed schema switch command");
                    
                    // Verify the schema switch worked
                    try (var rs = statement.executeQuery("SELECT current_schema()")) {
                        if (rs.next()) {
                            String currentSchema = rs.getString(1);
                            Log.infof("🎯 TEST: Current database schema verified: %s", currentSchema);
                            
                            if (!actualTenantId.equals(currentSchema)) {
                                Log.errorf("❌ TEST: Schema mismatch! Expected: %s, Actual: %s", actualTenantId, currentSchema);
                            }
                        }
                    }
                    
                    // Additional verification - check search_path
                    try (var rs = statement.executeQuery("SHOW search_path")) {
                        if (rs.next()) {
                            String searchPath = rs.getString(1);
                            Log.infof("🔍 TEST: Current search_path: %s", searchPath);
                        }
                    }
                }
            } else {
                Log.warnf("⚠️ TEST: No tenant identifier provided or empty, using default schema");
            }
        } catch (SQLException e) {
            Log.errorf(e, "💥 TEST: Failed to switch to schema: %s", actualTenantId);
            releaseConnection(tenantIdentifier, connection);
            throw e;
        }
        
        Log.infof("🚀 TEST MTCP: Returning connection configured for schema: %s", actualTenantId);
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // Reset to default schema before releasing
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO gocommerce, public");
            }
        } catch (SQLException e) {
            Log.warnf(e, "TEST: Failed to reset schema for tenant: %s", tenantIdentifier);
        }
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return dataSource.getClass().isAssignableFrom(unwrapType);
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return unwrapType.cast(dataSource);
        }
        throw new UnknownUnwrapTypeException(unwrapType);
    }
}
