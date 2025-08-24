package dev.tiodati.saas.gocommerce.test;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver;
import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * PostgreSQL-specific tenant connection resolver for tests that properly sets
 * the search_path for schema-based multi-tenancy.
 */
@ApplicationScoped
@Alternative
@Priority(1) // Highest priority to override production resolver
@Unremovable
@PersistenceUnitExtension
public class PostgreSQLTestTenantConnectionResolver implements TenantConnectionResolver {

    @Inject
    AgroalDataSource dataSource;

    @Override
    public ConnectionProvider resolve(String tenantId) {
        // Get the actual tenant from system property set by test extension
        String actualTenantId = System.getProperty("test.current.tenant", tenantId);
        Log.debugf("Resolving connection for tenant: %s (actual: %s)", tenantId, actualTenantId);
        
        return new ConnectionProvider() {
            @Override
            public Connection getConnection() throws SQLException {
                Connection connection = dataSource.getConnection();
                
                // Set the search_path to the tenant schema for PostgreSQL
                try (Statement statement = connection.createStatement()) {
                    // For public/platform operations, use gocommerce schema as that's where the store table lives
                    // For tenant operations, use the tenant schema with gocommerce as fallback
                    String searchPath;
                    if (actualTenantId == null || "public".equals(actualTenantId)) {
                        searchPath = "SET search_path TO gocommerce, public";
                    } else {
                        searchPath = String.format("SET search_path TO \"%s\", gocommerce, public", actualTenantId);
                    }
                    statement.execute(searchPath);
                    Log.debugf("Set search_path for tenant %s: %s", actualTenantId, searchPath);
                } catch (SQLException e) {
                    Log.errorf("Failed to set search_path for tenant %s: %s", actualTenantId, e.getMessage());
                    // Close the connection if we couldn't set the search_path
                    try {
                        connection.close();
                    } catch (SQLException closeException) {
                        e.addSuppressed(closeException);
                    }
                    throw e;
                }
                
                return connection;
            }

            @Override
            public void closeConnection(Connection connection) throws SQLException {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            }

            @Override
            public boolean supportsAggressiveRelease() {
                return false;
            }

            @Override
            public boolean isUnwrappableAs(Class<?> unwrapType) {
                return false;
            }

            @Override
            public <T> T unwrap(Class<T> unwrapType) {
                throw new UnsupportedOperationException("Unwrap not supported");
            }
        };
    }
}
