package dev.tiodati.saas.gocommerce.platform.dto;

import dev.tiodati.saas.gocommerce.platform.entity.StoreStatus;
import java.util.UUID;
import java.time.LocalDateTime; // Assuming createdAt and updatedAt might be needed

/**
 * Data Transfer Object for PlatformStore information. Used for representing
 * store data, typically as a response.
 *
 * @param id            The unique identifier of the store.
 * @param name          The name of the store.
 * @param subdomain     The subdomain of the store.
 * @param email         The contact email for the store.
 * @param currencyCode  The default currency code for the store.
 * @param defaultLocale The default locale for the store.
 * @param status        The current status of the store.
 * @param description   An optional description for the store.
 * @param fullDomain    The full domain of the store.
 * @param createdAt     Timestamp of creation.
 * @param updatedAt     Timestamp of last update.
 */
public record PlatformStoreDto(UUID id, String name, String subdomain,
        String email, String currencyCode, String defaultLocale,
        StoreStatus status, String description, // Added description based on
                                                // CreateStoreRequest
        String fullDomain, // Added based on StoreResponse
        LocalDateTime createdAt, // Added based on StoreResponse
        LocalDateTime updatedAt // Common field for DTOs representing entities
) {
    // Records automatically provide a canonical constructor, getters, equals(),
    // hashCode(), and toString().
    // No-arg constructor and setters are not conventional for records.
    // If default values or different construction logic is needed, factory
    // methods can be used,
    // or the service layer can handle DTO creation appropriately.
}
