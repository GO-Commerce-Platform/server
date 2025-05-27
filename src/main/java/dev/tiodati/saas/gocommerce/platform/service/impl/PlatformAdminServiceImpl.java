package dev.tiodati.saas.gocommerce.platform.service.impl;

import java.time.OffsetDateTime;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.exception.custom.DuplicateResourceException;
import dev.tiodati.saas.gocommerce.exception.custom.ResourceNotFoundException;
import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.api.dto.StoreResponse;
import dev.tiodati.saas.gocommerce.platform.api.dto.UpdateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.entity.PlatformStore;
import dev.tiodati.saas.gocommerce.platform.entity.StoreStatus;
import dev.tiodati.saas.gocommerce.platform.repository.PlatformStoreRepository;
import dev.tiodati.saas.gocommerce.platform.service.PlatformAdminService;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PlatformAdminServiceImpl implements PlatformAdminService {

    /**
     * Repository for managing platform stores. This repository handles
     * persistence operations for the {@link PlatformStore} entity.
     */
    private final PlatformStoreRepository storeRepository;

    @Inject
    public PlatformAdminServiceImpl(PlatformStoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Override
    @Transactional
    public StoreResponse createStore(CreateStoreRequest request) {
        Log.info("Creating new store: " + request.name() + " with subdomain: "
                + request.subdomain());

        if (storeRepository.existsBySubdomain(request.subdomain())) {
            Log.warn("Subdomain " + request.subdomain() + " already exists");
            throw new DuplicateResourceException("Store with subdomain '"
                    + request.subdomain() + "' already exists.");
        }

        try {
            OffsetDateTime now = OffsetDateTime.now();
            PlatformStore store = PlatformStore.builder().name(request.name())
                    .subdomain(request.subdomain()).email(request.email())
                    .currencyCode(request.currencyCode())
                    .defaultLocale(request.defaultLocale())
                    .description(request.description())
                    .domainSuffix("gocommerce.com")
                    .status(request.status() != null ? request.status()
                            : StoreStatus.PENDING)
                    .deleted(false).createdAt(now).updatedAt(now).build();

            storeRepository.persist(store);
            Log.info("Store entity persisted with ID: " + store.getId());

            // TODO: Create Keycloak realm for the store
            // TODO: Create store admin user in Keycloak
            // TODO: Create database schema for the store
            // schemaManager.createSchema("store_" +
            // store.getSubdomain().replace("-", "_"));

            if (store.getStatus() == StoreStatus.PENDING
                    && request.status() == StoreStatus.ACTIVE) {
                store.setStatus(StoreStatus.ACTIVE);
                storeRepository.persist(store);
            }

            Log.info("Store created successfully with ID: " + store.getId());

            return new StoreResponse(store.getId(), store.getName(),
                    store.getSubdomain(), store.getFullDomain(),
                    store.getStatus().toString(), store.getCreatedAt());
        } catch (PersistenceException e) {
            Log.error("Error creating store: " + e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to create store due to persistence error.", e);
        } catch (Exception e) {
            Log.error("Unexpected error creating store: " + e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to create store due to an unexpected error.", e);
        }
    }

    @Override
    @Transactional
    public StoreResponse updateStore(UUID storeId, UpdateStoreRequest request) {
        Log.info("Updating store with ID: " + storeId);

        PlatformStore store = storeRepository.findByIdOptional(storeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Store not found with ID: " + storeId));

        boolean updated = false;
        if (request.name() != null && !request.name().equals(store.getName())) {
            store.setName(request.name());
            updated = true;
        }
        if (request.email() != null
                && !request.email().equals(store.getEmail())) {
            store.setEmail(request.email());
            updated = true;
        }
        if (request.currencyCode() != null
                && !request.currencyCode().equals(store.getCurrencyCode())) {
            store.setCurrencyCode(request.currencyCode());
            updated = true;
        }
        if (request.defaultLocale() != null
                && !request.defaultLocale().equals(store.getDefaultLocale())) {
            store.setDefaultLocale(request.defaultLocale());
            updated = true;
        }
        if (request.status() != null && request.status() != store.getStatus()) {
            store.setStatus(request.status());
            updated = true;
        }
        if (request.description() != null
                && !request.description().equals(store.getDescription())) {
            store.setDescription(request.description());
            updated = true;
        }

        if (updated) {
            try {
                storeRepository.persist(store);
                Log.info("Store updated successfully: " + storeId);
            } catch (PersistenceException e) {
                Log.error("Error updating store " + storeId + ": "
                        + e.getMessage(), e);
                throw new RuntimeException(
                        "Failed to update store due to persistence error.", e);
            }
        } else {
            Log.info("No changes detected for store: " + storeId);
        }

        return new StoreResponse(store.getId(), store.getName(),
                store.getSubdomain(), store.getFullDomain(),
                store.getStatus().toString(), store.getCreatedAt());
    }

    @Override
    @Transactional
    public void deleteStore(UUID storeId) {
        Log.info("Attempting to delete store with ID: " + storeId);
        PlatformStore store = storeRepository.findByIdOptional(storeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Store not found with ID: " + storeId));

        store.setDeleted(true);
        store.setStatus(StoreStatus.INACTIVE);

        try {
            storeRepository.persist(store);
            Log.info("Store soft-deleted successfully: " + storeId);

            // TO DO: Implement cleanup logic:
            // - Delete Keycloak realm
            // - Drop database schema (schemaManager.dropSchema(...))
            // - Archive data if necessary
        } catch (PersistenceException e) {
            Log.error("Error soft-deleting store " + storeId + ": "
                    + e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to soft-delete store due to persistence error.", e);
        }
    }
}
