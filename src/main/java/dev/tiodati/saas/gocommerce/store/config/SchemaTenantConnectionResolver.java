package dev.tiodati.saas.gocommerce.store.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import io.agroal.api.AgroalDataSource;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Quarkus-native tenant connection resolver for SCHEMA multi-tenancy.
 * 
 * This implements the Quarkus TenantConnectionResolver abstraction (not raw Hibernate)
 * and properly switches PostgreSQL schemas using SET search_path.
 * 
 * Based on Quarkus documentation research:
 * - For SCHEMA strategy, we need TenantConnectionResolver with @PersistenceUnitExtension
 * - This is the correct Quarkus approach (not raw MultiTenantConnectionProvider)
 */
@ApplicationScoped
@PersistenceUnitExtension
public class SchemaTenantConnectionResolver implements TenantConnectionResolver {

    @PostConstruct
    public void init() {
        Log.infof("🎯 SCHEMA RESOLVER: SchemaTenantConnectionResolver initialized!");
    }

    @Inject
    private AgroalDataSource dataSource;

    @Override
    public ConnectionProvider resolve(String tenantId) {
        Log.infof("🔄 SCHEMA RESOLVER: *** RESOLVING CONNECTION *** for tenant: %s", tenantId);
        
        return new ConnectionProvider() {
            @Override
            public Connection getConnection() throws SQLException {
                Log.infof("🔌 SCHEMA RESOLVER: *** GETTING CONNECTION *** for tenant: %s", tenantId);
                Connection connection = dataSource.getConnection();
                
                // Switch to the tenant's schema using SET search_path
                try (Statement statement = connection.createStatement()) {
                    if (tenantId != null && !tenantId.trim().isEmpty() && !"public".equals(tenantId)) {
                        String searchPath = String.format("SET search_path TO \"%s\", public", tenantId);
                        Log.infof("🔀 SCHEMA RESOLVER: Executing: %s", searchPath);
                        statement.execute(searchPath);
                        Log.infof("✅ SCHEMA RESOLVER: Successfully switched to schema: %s", tenantId);
                    } else {
                        Log.infof("📋 SCHEMA RESOLVER: Using default schema (tenantId: %s)", tenantId);
                    }
                } catch (SQLException e) {
                    Log.errorf(e, "❌ SCHEMA RESOLVER: Failed to switch to schema: %s", tenantId);
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
