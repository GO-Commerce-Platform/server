package dev.tiodati.saas.gocommerce.platform.service.impl;

import java.time.Instant;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.exception.custom.DuplicateResourceException;
import dev.tiodati.saas.gocommerce.exception.custom.ResourceNotFoundException;
import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.api.dto.StoreResponse;
import dev.tiodati.saas.gocommerce.platform.api.dto.UpdateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.repository.PlatformStoreRepository;
import dev.tiodati.saas.gocommerce.platform.service.PlatformAdminService;
import dev.tiodati.saas.gocommerce.store.entity.Store;
import dev.tiodati.saas.gocommerce.store.entity.StoreStatus;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PlatformAdminServiceImpl implements PlatformAdminService {

    /**
     * Repository for managing platform stores. This repository handles
     * persistence operations for the {@link Store} entity.
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

        if (storeRepository.findBySubdomain(request.subdomain()).isPresent()) {
            Log.warn("Subdomain " + request.subdomain() + " already exists");
            throw new DuplicateResourceException("Store with subdomain '"
                    + request.subdomain() + "' already exists.");
        }
        if (storeRepository.findByName(request.name()).isPresent()) {
            Log.warn("Store name " + request.name() + " already exists");
            throw new DuplicateResourceException(
                    "Store with name '" + request.name() + "' already exists.");
        }

        try {
            Store store = Store.builder().name(request.name())
                    .subdomain(request.subdomain()).ownerId(request.ownerId())
                    .email(request.email()).currencyCode(request.currencyCode())
                    .defaultLocale(request.defaultLocale())
                    .description(request.description())
                    .domainSuffix("gocommerce.com")
                    // Set storeKey and schemaName
                    .storeKey(request.subdomain())
                    .schemaName("gocommerce_" + request.subdomain())
                    .status(request.status() != null ? request.status()
                            : StoreStatus.PENDING)
                    .createdAt(Instant.now()).updatedAt(Instant.now()).build();

            storeRepository.persist(store);
            Log.info("Store entity persisted with ID: " + store.getId());

            if (store.getStatus() == StoreStatus.PENDING
                    && request.status() == StoreStatus.ACTIVE) {
                store.setStatus(StoreStatus.ACTIVE);
                store.setUpdatedAt(Instant.now());
                storeRepository.persist(store);
            }

            Log.info("Store created successfully with ID: " + store.getId());

            return new StoreResponse(store.getId(), store.getOwnerId(),
                    store.getName(), store.getSubdomain(),
                    store.getFullDomain(), store.getStatus().toString(),
                    store.getCreatedAt(), store.getUpdatedAt());
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

        Store store = storeRepository.findByIdOptional(storeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Store not found with ID: " + storeId));

        boolean updated = false;
        if (request.name() != null && !request.name().equals(store.getName())) {
            // Check if new name already exists for another store
            if (storeRepository.findByName(request.name())
                    .filter(s -> !s.getId().equals(storeId)).isPresent()) {
                throw new DuplicateResourceException("Store with name '"
                        + request.name() + "' already exists.");
            }
            store.setName(request.name());
            updated = true;
        }
        if (request.ownerId() != null
                && !request.ownerId().equals(store.getOwnerId())) {
            store.setOwnerId(request.ownerId());
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
            store.setUpdatedAt(Instant.now());
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

        return new StoreResponse(store.getId(), store.getOwnerId(),
                store.getName(), store.getSubdomain(), store.getFullDomain(),
                store.getStatus().toString(), store.getCreatedAt(),
                store.getUpdatedAt());
    }

    @Override
    @Transactional
    public void deleteStore(UUID storeId) {
        Log.info("Attempting to delete store with ID: " + storeId);
        Store store = storeRepository.findByIdOptional(storeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Store not found with ID: " + storeId));

        store.setStatus(StoreStatus.DELETING);
        store.setUpdatedAt(Instant.now());

        try {
            storeRepository.persist(store);
            Log.info("Store status set to " + store.getStatus() + " for ID: "
                    + storeId);

        } catch (PersistenceException e) {
            Log.error("Error processing store deletion " + storeId + ": "
                    + e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to process store deletion due to persistence error.",
                    e);
        }
    }
}
