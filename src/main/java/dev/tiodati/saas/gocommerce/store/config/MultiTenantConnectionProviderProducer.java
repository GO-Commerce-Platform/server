package dev.tiodati.saas.gocommerce.store.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

@ApplicationScoped
public class MultiTenantConnectionProviderProducer {

    @Inject
    private SchemaTenantConnectionProvider schemaTenantConnectionProvider;

    @Produces
    @ApplicationScoped
    public MultiTenantConnectionProvider<String> multiTenantConnectionProvider() {
        return schemaTenantConnectionProvider;
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
