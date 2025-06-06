package dev.tiodati.saas.gocommerce.store.service;

import dev.tiodati.saas.gocommerce.exception.custom.DuplicateResourceException;
import dev.tiodati.saas.gocommerce.resource.dto.CreateStoreDto;
import dev.tiodati.saas.gocommerce.store.entity.Store;

/**
 * Service interface for orchestrating the complete store creation workflow.
 * This service coordinates all necessary steps to create a new store including
 * schema creation, Keycloak setup, configuration, and registration.
 */
public interface StoreCreationService {

    /**
     * Creates a new store by orchestrating all required setup steps.
     *
     * @param createStoreDto the store creation request containing all necessary
     *                       information
     * @return the created and persisted Store entity
     * @throws DuplicateResourceException if a store with the same key or subdomain
     *                                    already exists
     */
    Store createStore(CreateStoreDto createStoreDto) throws DuplicateResourceException;
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
