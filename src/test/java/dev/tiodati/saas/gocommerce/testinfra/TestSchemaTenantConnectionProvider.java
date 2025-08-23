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

/**
 * Test-specific multi-tenant connection provider for schema-based tenancy.
 * This provider switches the database schema based on the tenant ID and has
 * higher priority than the production provider.
 */
@ApplicationScoped
@Alternative
@Priority(1) // Higher priority than production provider
@Unremovable
public class TestSchemaTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    @Inject
    private AgroalDataSource dataSource;

    @Inject
    private TestTenantContext testTenantContext;

    @Override
    public Connection getAnyConnection() throws SQLException {
        Log.debugf("TEST MTCP: getAnyConnection called");
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        // Use TestTenantContext to get the actual tenant if available
        String actualTenantId = tenantIdentifier;
        if (testTenantContext != null && testTenantContext.getCurrentTenant() != null) {
            actualTenantId = testTenantContext.getCurrentTenant();
            Log.infof("TEST MTCP: Using tenant from TestTenantContext: %s (requested: %s)", actualTenantId, tenantIdentifier);
        } else {
            Log.infof("TEST MTCP: Using requested tenant: %s (TestTenantContext not available or empty)", tenantIdentifier);
        }
        
        Log.infof("TEST MTCP: getConnection called with tenant: %s (actual: %s)", tenantIdentifier, actualTenantId);
        Connection connection = getAnyConnection();
        try {
            // Switch to the tenant's schema
            if (actualTenantId != null && !actualTenantId.trim().isEmpty()) {
                Log.infof("TEST: Switching database schema to: %s", actualTenantId);
                try (Statement statement = connection.createStatement()) {
                    // Use quoted identifier to handle special characters in schema names
                    String sql = String.format("SET search_path TO \"%s\", public", actualTenantId);
                    statement.execute(sql);
                    Log.infof("TEST: Successfully switched to schema: %s", actualTenantId);
                    
                    // Verify the schema switch worked
                    try (var rs = statement.executeQuery("SELECT current_schema()")) {
                        if (rs.next()) {
                            String currentSchema = rs.getString(1);
                            Log.infof("TEST: Current database schema is: %s", currentSchema);
                        }
                    }
                }
            } else {
                Log.warnf("TEST: No tenant identifier provided or empty, using default schema");
            }
        } catch (SQLException e) {
            Log.errorf(e, "TEST: Failed to switch to schema: %s", actualTenantId);
            releaseConnection(tenantIdentifier, connection);
            throw e;
        }
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
