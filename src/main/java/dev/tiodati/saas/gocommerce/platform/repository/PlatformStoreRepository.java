package dev.tiodati.saas.gocommerce.platform.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import dev.tiodati.saas.gocommerce.platform.entity.PlatformStores;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class PlatformStoreRepository implements PanacheRepositoryBase<PlatformStores, UUID> {

    public Optional<PlatformStores> findBySubdomain(String subdomain) {
        return find("subdomain = ?1 AND isDeleted = false", subdomain).firstResultOptional();
    }

    public boolean existsBySubdomain(String subdomain) {
        return count("subdomain = ?1", subdomain) > 0;
    }
}
