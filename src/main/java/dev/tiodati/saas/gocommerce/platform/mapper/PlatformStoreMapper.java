package dev.tiodati.saas.gocommerce.platform.mapper;

import dev.tiodati.saas.gocommerce.platform.api.dto.StoreResponse;
import dev.tiodati.saas.gocommerce.store.entity.Store; // Updated import

public final class PlatformStoreMapper {

    private PlatformStoreMapper() {
        // Utility class, not meant to be instantiated
    }

    public static StoreResponse toResponse(Store store) {

        if (store == null) {
            return null;
        }

        return new StoreResponse(
                store.getId(),
                store.getOwnerId(),
                store.getName(),
                store.getSubdomain(),
                store.getFullDomain(),
                store.getStatus().name(),
                store.getCreatedAt(),
                store.getUpdatedAt()
        );
    }
}
