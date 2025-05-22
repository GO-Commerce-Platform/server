package dev.tiodati.saas.gocommerce.platform.service;

import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.api.dto.StoreResponse;

public interface PlatformAdminService {
    /**
     * Creates a new store in the platform.
     *
     * @param request The store creation request containing store details and admin user information
     * @return Response containing the created store information
     */
    StoreResponse createStore(CreateStoreRequest request);
}
