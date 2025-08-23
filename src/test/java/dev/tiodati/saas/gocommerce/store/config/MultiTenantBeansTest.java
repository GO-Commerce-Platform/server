package dev.tiodati.saas.gocommerce.store.config;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.test.junit.QuarkusTest;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test to verify that both multi-tenant beans are discoverable by CDI.
 */
@QuarkusTest
public class MultiTenantBeansTest {

    @Inject
    @PersistenceUnitExtension
    TenantResolver tenantResolver;
    
    @Inject
    MultiTenantConnectionProvider<String> connectionProvider;
    
    @Test
    void testBothBeansAreAvailable() {
        assertNotNull(tenantResolver, "TenantResolver should be injectable");
        assertNotNull(connectionProvider, "MultiTenantConnectionProvider should be injectable");
        
        System.out.println("TenantResolver class: " + tenantResolver.getClass().getSimpleName());
        System.out.println("ConnectionProvider class: " + connectionProvider.getClass().getSimpleName());
        
        // Test that they actually work
        String tenantId = tenantResolver.resolveTenantId();
        System.out.println("Resolved tenant ID: " + tenantId);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
