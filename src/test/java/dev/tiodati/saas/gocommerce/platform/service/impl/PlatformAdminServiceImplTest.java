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
import dev.tiodati.saas.gocommerce.store.entity.Store; // Import the new Store entity
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
        String ownerId = UUID.randomUUID().toString();
        CreateStoreRequest uniqueCreateRequest = new CreateStoreRequest(
                "Success Store",
                "success-store-" + UUID.randomUUID().toString().substring(0, 8),
                ownerId, // Added ownerId
                "success@store.com", "USD", "en-US", StoreStatus.ACTIVE,
                "A success store description.");

        StoreResponse createdStoreResponse = platformAdminService
                .createStore(uniqueCreateRequest);

        assertNotNull(createdStoreResponse);
        assertNotNull(createdStoreResponse.id());
        assertEquals(uniqueCreateRequest.name(), createdStoreResponse.name()); // Changed
                                                                               // to
                                                                               // name()
        assertEquals(uniqueCreateRequest.subdomain(),
                createdStoreResponse.subdomain());
        assertEquals(uniqueCreateRequest.ownerId(),
                createdStoreResponse.ownerId()); // Assert ownerId
        assertEquals(uniqueCreateRequest.status().toString(),
                createdStoreResponse.status());
        assertNotNull(createdStoreResponse.createdAt());
        assertNotNull(createdStoreResponse.updatedAt());

        Optional<Store> persistedStoreOpt = platformStoreRepository // Changed
                                                                    // to Store
                .findByIdOptional(createdStoreResponse.id());
        assertTrue(persistedStoreOpt.isPresent());
        Store persistedStore = persistedStoreOpt.get(); // Changed to Store
        assertEquals(uniqueCreateRequest.name(), persistedStore.getName()); // Changed
                                                                            // to
                                                                            // getName()
        assertEquals(uniqueCreateRequest.subdomain(),
                persistedStore.getSubdomain());
        assertEquals(uniqueCreateRequest.ownerId(),
                persistedStore.getOwnerId()); // Assert ownerId
        assertEquals(uniqueCreateRequest.email(), persistedStore.getEmail());
        assertEquals(uniqueCreateRequest.status(), persistedStore.getStatus()); // Assert
                                                                                // status
        assertNotNull(persistedStore.getCreatedAt());
        assertNotNull(persistedStore.getUpdatedAt());
        // assertEquals(uniqueCreateRequest.domainSuffix(),
        // persistedStore.getDomainSuffix()); // This line would fail if
        // domainSuffix is not in DTO or automatically set in a predictable way
    }

    @Test
    @Transactional
    void testCreateStoreDuplicateSubdomain() {
        String ownerId = UUID.randomUUID().toString();
        String duplicateSubdomain = "duplicate-store-"
                + UUID.randomUUID().toString().substring(0, 8);
        // Arrange: Create and persist an initial store
        CreateStoreRequest initialRequest = new CreateStoreRequest(
                "Initial Store", duplicateSubdomain, ownerId,
                "initial@store.com", "USD", "en-US", StoreStatus.ACTIVE,
                "Initial store description.");
        platformAdminService.createStore(initialRequest); // Persist the first
                                                          // store

        // Prepare another request with the same subdomain
        CreateStoreRequest duplicateCreateRequest = new CreateStoreRequest(
                "Duplicate Store", duplicateSubdomain, // Same subdomain
                UUID.randomUUID().toString(), // Different ownerId
                "duplicate@store.com", "EUR", "fr-FR", StoreStatus.PENDING,
                "Duplicate store description."); // Removed domainSuffix

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> platformAdminService.createStore(duplicateCreateRequest));

        assertEquals("Store with subdomain '" + duplicateSubdomain
                + "' already exists.", exception.getMessage());
    }

    @Test
    @Transactional
    void testUpdateStoreSuccess() {
        String initialOwnerId = UUID.randomUUID().toString();
        String uniqueSubdomainForUpdate = "update-test-store-"
                + UUID.randomUUID().toString().substring(0, 8);
        // Arrange: Persist an initial store using CreateStoreRequest
        StoreResponse createdInitialStore = platformAdminService
                .createStore(new CreateStoreRequest("Original Store Name",
                        uniqueSubdomainForUpdate, initialOwnerId,
                        "update@store.com", "USD", "en-US", StoreStatus.ACTIVE,
                        "Original desc"));
        UUID storeId = createdInitialStore.id();

        String updatedOwnerId = UUID.randomUUID().toString();
        // Prepare DTO for update using UpdateStoreRequest
        UpdateStoreRequest updateRequest = new UpdateStoreRequest(
                "Updated Store Name", updatedOwnerId, "updated@store.com",
                "EUR", "fr-FR", StoreStatus.INACTIVE, "Updated description");

        // Act
        StoreResponse updatedStoreResponse = platformAdminService
                .updateStore(storeId, updateRequest); // Use UpdateStoreRequest

        // Assert
        assertNotNull(updatedStoreResponse);
        assertEquals(storeId, updatedStoreResponse.id());
        assertEquals("Updated Store Name", updatedStoreResponse.name()); // Changed
                                                                         // to
                                                                         // name()
        assertEquals(updatedOwnerId, updatedStoreResponse.ownerId()); // Assert
                                                                      // ownerId
        assertEquals(StoreStatus.INACTIVE.toString(),
                updatedStoreResponse.status()); // Assert against String status
        assertNotNull(updatedStoreResponse.updatedAt());
        // Ensure createdAt is still the original one
        assertEquals(createdInitialStore.createdAt(),
                updatedStoreResponse.createdAt());

        Optional<Store> fetchedStoreOpt = platformStoreRepository // Changed to
                                                                  // Store
                .findByIdOptional(storeId);
        assertTrue(fetchedStoreOpt.isPresent());
        Store fetchedStore = fetchedStoreOpt.get(); // Changed to Store
        assertEquals("Updated Store Name", fetchedStore.getName()); // Changed
                                                                    // to
                                                                    // getName()
        assertEquals(updatedOwnerId, fetchedStore.getOwnerId()); // Assert
                                                                 // ownerId
        assertEquals(StoreStatus.INACTIVE, fetchedStore.getStatus());
        assertEquals("updated@store.com", fetchedStore.getEmail());
        assertEquals("Updated description", fetchedStore.getDescription());
        assertNotNull(fetchedStore.getUpdatedAt());
        // Ensure createdAt is not changed by update
        assertEquals(createdInitialStore.createdAt(),
                fetchedStore.getCreatedAt());
    }

    @Test
    @Transactional
    void testUpdateStoreNotFound() { // Renamed method
        // Arrange: A non-existent UUID
        UUID nonExistentId = UUID.randomUUID();
        String dummyOwnerId = UUID.randomUUID().toString(); // Added dummy ownerId
        // Use UpdateStoreRequest for the update payload
        UpdateStoreRequest updatePayload = new UpdateStoreRequest(
                "Non Existent Update", dummyOwnerId, "non@existent.com", "USD", "en-US", // Added dummyOwnerId
                StoreStatus.ACTIVE, "Non existent description");

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class, () -> platformAdminService
                        .updateStore(nonExistentId, updatePayload)); // Use
                                                                     // UpdateStoreRequest

        assertEquals("Store not found with ID: " + nonExistentId,
                exception.getMessage());
    }

    @Test
    @Transactional
    void testDeleteStoreSuccess() {
        String ownerId = UUID.randomUUID().toString();
        String uniqueSubdomainForDelete = "delete-me-store-"
                + UUID.randomUUID().toString().substring(0, 8);
        // Arrange: Persist a store to delete using CreateStoreRequest
        StoreResponse createdStoreToDelete = platformAdminService
                .createStore(new CreateStoreRequest("Store To Delete",
                        uniqueSubdomainForDelete, ownerId, "delete@store.com",
                        "USD", "en-US", StoreStatus.ACTIVE, "Desc to delete"));
        UUID storeId = createdStoreToDelete.id();

        // Act
        platformAdminService.deleteStore(storeId);

        // Assert: Verify the store is marked as DELETING (or DELETED based on
        // service impl)
        Optional<Store> deletedStoreOpt = platformStoreRepository // Changed to
                                                                  // Store
                .findByIdOptional(storeId);
        assertTrue(deletedStoreOpt.isPresent(),
                "Store should still be found in the database.");
        // Assuming deleteStore in PlatformAdminServiceImpl sets status to
        // DELETING
        assertEquals(StoreStatus.DELETING, deletedStoreOpt.get().getStatus(),
                "Store's status should be DELETING after soft deletion.");
        assertNotNull(deletedStoreOpt.get().getUpdatedAt());

    }

    @Test
    @Transactional
    void testDeleteStoreNotFound() { // Renamed method
        // Arrange: A non-existent UUID
        UUID nonExistentId = UUID.randomUUID();

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> platformAdminService.deleteStore(nonExistentId));

        assertEquals("Store not found with ID: " + nonExistentId,
                exception.getMessage());
    }
}
