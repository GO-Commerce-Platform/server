package dev.tiodati.saas.gocommerce.platform.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import dev.tiodati.saas.gocommerce.store.entity.Store;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class PlatformStoreRepository
        implements PanacheRepositoryBase<Store, UUID> {

    public Optional<Store> findBySubdomain(String subdomain) {
        return find("subdomain", subdomain).firstResultOptional();
    }

    public Optional<Store> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    // find by keycloakRealmId
    public Optional<Store> findByKeycloakRealmId(String keycloakRealmId) {
        return find("keycloakRealmId", keycloakRealmId).firstResultOptional();
    }

}
