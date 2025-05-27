package dev.tiodati.saas.gocommerce.platform.mapper;

import dev.tiodati.saas.gocommerce.platform.api.dto.StoreResponse;
import dev.tiodati.saas.gocommerce.platform.entity.PlatformStores;

public final class PlatformStoreMapper {

    private PlatformStoreMapper() {
        // Utility class, not meant to be instantiated
    }

    public static StoreResponse toResponse(PlatformStores store) {
        if (store == null) {
            return null;
        }

        return new StoreResponse(store.getId(), store.getStoreName(),
                store.getSubdomain(), store.getFullDomain(),
                store.getStatus().toString(), store.getCreatedAt());
    }
}
