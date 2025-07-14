package dev.tiodati.saas.gocommerce.store.config;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;

import io.agroal.api.AgroalDataSource;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Multi-tenant connection provider for schema-based tenancy.
 * This provider switches the database schema based on the tenant ID.
 */
@ApplicationScoped
public class SchemaTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    @Inject
    private AgroalDataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        Log.infof("MTCP: getAnyConnection called");
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Log.infof("MTCP: getConnection called with tenant: %s", tenantIdentifier);
        Connection connection = getAnyConnection();
        try {
            // Switch to the tenant's schema
            if (tenantIdentifier != null && !tenantIdentifier.trim().isEmpty()) {
                Log.infof("Switching database schema to: %s", tenantIdentifier);
                connection.createStatement().execute("SET search_path TO " + tenantIdentifier);
                Log.infof("Successfully switched to schema: %s", tenantIdentifier);
            } else {
                Log.warnf("No tenant identifier provided or empty, using default schema");
            }
        } catch (SQLException e) {
            Log.errorf(e, "Failed to switch to schema: %s", tenantIdentifier);
            releaseConnection(tenantIdentifier, connection);
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // Reset to default schema before releasing
            connection.createStatement().execute("SET search_path TO gocommerce");
        } catch (SQLException e) {
            Log.warnf(e, "Failed to reset schema for tenant: %s", tenantIdentifier);
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

// Copilot: This file may have been generated or refactored by GitHub Copilot.
