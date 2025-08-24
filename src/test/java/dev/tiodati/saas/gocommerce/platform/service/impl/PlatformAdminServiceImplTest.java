package dev.tiodati.saas.gocommerce.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.exception.custom.DuplicateResourceException;
import dev.tiodati.saas.gocommerce.exception.custom.ResourceNotFoundException;
import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.api.dto.StoreResponse; // Import StoreResponse
import dev.tiodati.saas.gocommerce.platform.api.dto.UpdateStoreRequest; // Import UpdateStoreRequest
import dev.tiodati.saas.gocommerce.platform.repository.PlatformStoreRepository;
import dev.tiodati.saas.gocommerce.platform.service.PlatformAdminService;
import dev.tiodati.saas.gocommerce.store.entity.StoreStatus;
import dev.tiodati.saas.gocommerce.store.entity.Store;
import dev.tiodati.saas.gocommerce.shared.test.TestDataFactory;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
public class PlatformAdminServiceImplTest {

    /**
     * Real repository for PlatformStore entities.
     */
    @Inject
    private PlatformStoreRepository platformStoreRepository;

    /**
     * Service under test for platform administration functionalities.
     */
    @Inject
    private PlatformAdminService platformAdminService;

    // @QuarkusMock static factory methods removed

    @BeforeEach
    void setUp() {
        // Initialization of createStoreRequest removed as it was not used.
        // Test methods create their own specific CreateStoreRequest instances.
    }

    @Test
    @Transactional
    void testCreateStoreSuccess() {
        CreateStoreRequest createRequest = TestDataFactory.createStoreRequest();

        StoreResponse createdStoreResponse = platformAdminService
                .createStore(createRequest);

        assertNotNull(createdStoreResponse);
        assertNotNull(createdStoreResponse.id());
        assertEquals(createRequest.name(), createdStoreResponse.name());
        assertEquals(createRequest.subdomain(),
                createdStoreResponse.subdomain());
        assertEquals(createRequest.ownerId(),
                createdStoreResponse.ownerId()); // Assert ownerId
        assertEquals(createRequest.status().toString(),
                createdStoreResponse.status());
        assertNotNull(createdStoreResponse.createdAt());
        assertNotNull(createdStoreResponse.updatedAt());

        Optional<Store> persistedStoreOpt = platformStoreRepository
                .findByIdOptional(createdStoreResponse.id());
        assertTrue(persistedStoreOpt.isPresent());
        Store persistedStore = persistedStoreOpt.get();
        assertEquals(createRequest.name(), persistedStore.getName());
        assertEquals(createRequest.subdomain(),
                persistedStore.getSubdomain());
        assertEquals(createRequest.ownerId(),
                persistedStore.getOwnerId()); // Assert ownerId
        assertEquals(createRequest.email(), persistedStore.getEmail());
        assertEquals(createRequest.status(), persistedStore.getStatus());
        assertNotNull(persistedStore.getCreatedAt());
        assertNotNull(persistedStore.getUpdatedAt());
    }

    @Test
    @Transactional
    void testCreateStoreDuplicateSubdomain() {
        CreateStoreRequest initialRequest = TestDataFactory.createStoreRequest();
        platformAdminService.createStore(initialRequest);

        String uniqueName = "Duplicate Store " + UUID.randomUUID().toString().substring(0, 8);
        CreateStoreRequest duplicateCreateRequest = new CreateStoreRequest(
                uniqueName,
                initialRequest.subdomain(), // Same subdomain
                UUID.randomUUID().toString(),
                "duplicate@store.com",
                "EUR",
                "fr-FR",
                StoreStatus.PENDING,
                "Duplicate store description."
        );

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> platformAdminService.createStore(duplicateCreateRequest));

        assertEquals("Store with subdomain '" + initialRequest.subdomain()
                + "' already exists.", exception.getMessage());
    }

    @Test
    @Transactional
    void testUpdateStoreSuccess() {
        StoreResponse createdInitialStore = platformAdminService.createStore(TestDataFactory.createStoreRequest());
        UUID storeId = createdInitialStore.id();

        String updatedName = "Updated Store Name " + UUID.randomUUID().toString().substring(0, 8);
        String updatedOwnerId = UUID.randomUUID().toString();
        UpdateStoreRequest updateRequest = new UpdateStoreRequest(
                updatedName, updatedOwnerId, "updated@store.com",
                "EUR", "fr-FR", StoreStatus.INACTIVE, "Updated description");

        StoreResponse updatedStoreResponse = platformAdminService
                .updateStore(storeId, updateRequest);

        assertNotNull(updatedStoreResponse);
        assertEquals(storeId, updatedStoreResponse.id());
        assertEquals(updatedName, updatedStoreResponse.name());
        assertEquals(updatedOwnerId, updatedStoreResponse.ownerId());
        assertEquals(StoreStatus.INACTIVE.toString(),
                updatedStoreResponse.status());
        assertNotNull(updatedStoreResponse.updatedAt());
        assertEquals(createdInitialStore.createdAt(),
                updatedStoreResponse.createdAt());

        Optional<Store> fetchedStoreOpt = platformStoreRepository
                .findByIdOptional(storeId);
        assertTrue(fetchedStoreOpt.isPresent());
        Store fetchedStore = fetchedStoreOpt.get();
        assertEquals(updatedName, fetchedStore.getName());
        assertEquals(updatedOwnerId, fetchedStore.getOwnerId());
        assertEquals(StoreStatus.INACTIVE, fetchedStore.getStatus());
        assertEquals("updated@store.com", fetchedStore.getEmail());
        assertEquals("Updated description", fetchedStore.getDescription());
        assertNotNull(fetchedStore.getUpdatedAt());
        assertEquals(createdInitialStore.createdAt(),
                fetchedStore.getCreatedAt());
    }

    @Test
    @Transactional
    void testUpdateStoreNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        String dummyOwnerId = UUID.randomUUID().toString();
        UpdateStoreRequest updatePayload = new UpdateStoreRequest(
                "Non Existent Update", dummyOwnerId, "non@existent.com", "USD", "en-US",
                StoreStatus.ACTIVE, "Non existent description");

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class, () -> platformAdminService
                        .updateStore(nonExistentId, updatePayload));

        assertEquals("Store not found with ID: " + nonExistentId,
                exception.getMessage());
    }

    @Test
    @Transactional
    void testDeleteStoreSuccess() {
        StoreResponse createdStoreToDelete = platformAdminService.createStore(TestDataFactory.createStoreRequest());
        UUID storeId = createdStoreToDelete.id();

        platformAdminService.deleteStore(storeId);

        Optional<Store> deletedStoreOpt = platformStoreRepository
                .findByIdOptional(storeId);
        assertTrue(deletedStoreOpt.isPresent(),
                "Store should still be found in the database.");
        assertEquals(StoreStatus.DELETING, deletedStoreOpt.get().getStatus(),
                "Store's status should be DELETING after soft deletion.");
        assertNotNull(deletedStoreOpt.get().getUpdatedAt());
    }

    @Test
    @Transactional
    void testDeleteStoreNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> platformAdminService.deleteStore(nonExistentId));

        assertEquals("Store not found with ID: " + nonExistentId,
                exception.getMessage());
    }
}
