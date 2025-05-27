package dev.tiodati.saas.gocommerce.platform.service;

import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.api.dto.StoreResponse;
import dev.tiodati.saas.gocommerce.platform.api.dto.UpdateStoreRequest; // Import UpdateStoreRequest
import java.util.UUID; // Import UUID

/**
 * Service interface for platform administration tasks, such as managing stores.
 */
public interface PlatformAdminService {

    /**
     * Creates a new platform store.
     *
     * @param request The request DTO containing store creation details.
     * @return A DTO representing the newly created store.
     */
    StoreResponse createStore(CreateStoreRequest request);

    /**
     * Updates an existing platform store.
     *
     * @param storeId The UUID of the store to update.
     * @param request The request DTO containing store update details.
     * @return A DTO representing the updated store.
     */
    StoreResponse updateStore(UUID storeId, UpdateStoreRequest request);

    /**
     * Deletes a platform store by its ID. Implementations should handle soft
     * deletion.
     *
     * @param storeId The UUID of the store to delete.
     */
    void deleteStore(UUID storeId);

    // Potentially other methods like getStoreById, listStores, etc.
}
