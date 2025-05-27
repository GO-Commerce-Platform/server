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
import dev.tiodati.saas.gocommerce.platform.entity.PlatformStore;
import dev.tiodati.saas.gocommerce.platform.entity.StoreStatus;
import dev.tiodati.saas.gocommerce.platform.repository.PlatformStoreRepository;
import dev.tiodati.saas.gocommerce.platform.service.PlatformAdminService;
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
        // Use a unique subdomain for this test to avoid conflicts
        CreateStoreRequest uniqueCreateRequest = new CreateStoreRequest(
                "Success Store",
                "success-store-" + UUID.randomUUID().toString().substring(0, 8),
                "success@store.com", "USD", "en-US", StoreStatus.ACTIVE,
                "A success store description."); // Removed domainSuffix

        StoreResponse createdStoreResponse = platformAdminService
                .createStore(uniqueCreateRequest);

        assertNotNull(createdStoreResponse);
        assertNotNull(createdStoreResponse.id());
        assertEquals(uniqueCreateRequest.name(), createdStoreResponse.name());
        assertEquals(uniqueCreateRequest.subdomain(),
                createdStoreResponse.subdomain());
        assertEquals(uniqueCreateRequest.status().toString(),
                createdStoreResponse.status());

        Optional<PlatformStore> persistedStoreOpt = platformStoreRepository
                .findByIdOptional(createdStoreResponse.id());
        assertTrue(persistedStoreOpt.isPresent());
        PlatformStore persistedStore = persistedStoreOpt.get();
        assertEquals(uniqueCreateRequest.name(), persistedStore.getName());
        assertEquals(uniqueCreateRequest.subdomain(),
                persistedStore.getSubdomain());
        assertEquals(uniqueCreateRequest.email(), persistedStore.getEmail());
        // assertEquals(uniqueCreateRequest.domainSuffix(),
        // persistedStore.getDomainSuffix()); // This line would fail if
        // domainSuffix is not in DTO
    }

    @Test
    @Transactional
    void testCreateStoreDuplicateSubdomain() {
        String duplicateSubdomain = "duplicate-store-"
                + UUID.randomUUID().toString().substring(0, 8);
        // Arrange: Create and persist an initial store
        CreateStoreRequest initialRequest = new CreateStoreRequest(
                "Initial Store", duplicateSubdomain, "initial@store.com", "USD",
                "en-US", StoreStatus.ACTIVE, "Initial store description."); // Removed
                                                                            // domainSuffix
        platformAdminService.createStore(initialRequest); // Persist the first
                                                          // store

        // Prepare another request with the same subdomain
        CreateStoreRequest duplicateCreateRequest = new CreateStoreRequest(
                "Duplicate Store", duplicateSubdomain, // Same subdomain
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
        String uniqueSubdomainForUpdate = "update-test-store-"
                + UUID.randomUUID().toString().substring(0, 8);
        // Arrange: Persist an initial store using CreateStoreRequest
        StoreResponse createdInitialStore = platformAdminService
                .createStore(new CreateStoreRequest("Original Store Name",
                        uniqueSubdomainForUpdate, "update@store.com", "USD",
                        "en-US", StoreStatus.ACTIVE, "Original desc")); // Removed
                                                                        // domainSuffix
        UUID storeId = createdInitialStore.id();

        // Prepare DTO for update using UpdateStoreRequest
        UpdateStoreRequest updateRequest = new UpdateStoreRequest(
                "Updated Store Name", "updated@store.com", "EUR", "fr-FR",
                StoreStatus.INACTIVE, "Updated description");

        // Act
        StoreResponse updatedStoreResponse = platformAdminService
                .updateStore(storeId, updateRequest); // Use UpdateStoreRequest

        // Assert
        assertNotNull(updatedStoreResponse);
        assertEquals("Updated Store Name", updatedStoreResponse.name());
        assertEquals(StoreStatus.INACTIVE.toString(),
                updatedStoreResponse.status()); // Assert against String status
        // Assertions for email, description are removed as they are not in
        // StoreResponse
        // updatedStoreResponse.email() would not exist if StoreResponse doesn't
        // have it.

        Optional<PlatformStore> fetchedStoreOpt = platformStoreRepository
                .findByIdOptional(storeId); // Changed to findByIdOptional
        assertTrue(fetchedStoreOpt.isPresent());
        PlatformStore fetchedStore = fetchedStoreOpt.get();
        assertEquals("Updated Store Name", fetchedStore.getName());
        assertEquals(StoreStatus.INACTIVE, fetchedStore.getStatus());
        assertEquals("updated@store.com", fetchedStore.getEmail());
        assertEquals("Updated description", fetchedStore.getDescription());
    }

    @Test
    @Transactional
    void testUpdateStoreNotFound() { // Renamed method
        // Arrange: A non-existent UUID
        UUID nonExistentId = UUID.randomUUID();
        // Use UpdateStoreRequest for the update payload
        UpdateStoreRequest updatePayload = new UpdateStoreRequest(
                "Non Existent Update", "non@existent.com", "USD", "en-US",
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
        String uniqueSubdomainForDelete = "delete-me-store-"
                + UUID.randomUUID().toString().substring(0, 8);
        // Arrange: Persist a store to delete using CreateStoreRequest
        StoreResponse createdStoreToDelete = platformAdminService
                .createStore(new CreateStoreRequest("Store To Delete",
                        uniqueSubdomainForDelete, "delete@store.com", "USD",
                        "en-US", StoreStatus.ACTIVE, "Desc to delete")); // Removed
                                                                         // domainSuffix
        UUID storeId = createdStoreToDelete.id();

        // Act
        platformAdminService.deleteStore(storeId);

        // Assert: Verify the store is marked as deleted (soft delete)
        Optional<PlatformStore> deletedStoreOpt = platformStoreRepository
                .findByIdOptional(storeId);
        assertTrue(deletedStoreOpt.isPresent(),
                "Store should still be found in the database for soft delete.");
        assertTrue(deletedStoreOpt.get().isDeleted(),
                "Store's 'deleted' flag should be true after soft deletion.");

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
