package dev.tiodati.saas.gocommerce.shared.test;

import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.store.entity.StoreStatus;

import java.util.UUID;

public class TestDataFactory {

    public static CreateStoreRequest createStoreRequest() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return new CreateStoreRequest(
                "Test Store " + uniqueId,
                "test-store-" + uniqueId,
                UUID.randomUUID().toString(),
                "test.store." + uniqueId + "@example.com",
                "USD",
                "en-US",
                StoreStatus.ACTIVE,
                "A test store description."
        );
    }
}
