package dev.tiodati.saas.gocommerce.platform.api.dto;

import dev.tiodati.saas.gocommerce.store.entity.StoreStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for updating an existing PlatformStore. Fields are
 * optional; only non-null fields in the request will be considered for update.
 * The 'subdomain' is typically not updatable or requires special handling.
 *
 * @param name          The new name of the store. Max length 100.
 * @param ownerId       The new owner ID for the store.
 * @param email         The new administrative email for the store. Must be a
 *                      valid email format. Max length 255.
 * @param currencyCode  The new default currency code for the store (e.g.,
 *                      "USD"). Length 3.
 * @param defaultLocale The new default locale for the store (e.g., "en-US").
 *                      Max length 10.
 * @param status        The new status of the store.
 * @param description   A new optional description for the store. Max length
 *                      255.
 */
public record UpdateStoreRequest(
        @Size(max = 100, message = "Store name cannot exceed 100 characters.") String name,

        String ownerId, // Added ownerId, validation if needed (e.g., @UUID or
                        // specific format)

        // Subdomain is generally not updatable due to its critical nature for
        // routing and identity.
        // If it needs to be updatable, it requires careful consideration and
        // validation.
        // String subdomain,

        @Email(message = "Email should be valid.") @Size(max = 255, message = "Email cannot exceed 255 characters.") String email,

        @Size(min = 3, max = 3, message = "Currency code must be 3 characters long (e.g., USD).") String currencyCode,

        @Size(max = 10, message = "Default locale cannot exceed 10 characters (e.g., en-US).") String defaultLocale,

        StoreStatus status,

        @Size(max = 255, message = "Description cannot exceed 255 characters.") String description) {
}
