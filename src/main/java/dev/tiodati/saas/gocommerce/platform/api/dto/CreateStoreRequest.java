package dev.tiodati.saas.gocommerce.platform.api.dto;

import dev.tiodati.saas.gocommerce.platform.entity.StoreStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating a new PlatformStore. This DTO is typically
 * used in API request payloads.
 *
 * @param name          The name of the store. Must not be blank and have a max
 *                      length of 100.
 * @param subdomain     The subdomain for the store. Must not be blank, max
 *                      length 63, and follow domain naming conventions.
 * @param email         The administrative email for the store. Must be a valid
 *                      email format.
 * @param currencyCode  The default currency code for the store (e.g., "USD").
 *                      Must not be blank, length 3.
 * @param defaultLocale The default locale for the store (e.g., "en-US"). Must
 *                      not be blank.
 * @param status        The initial status of the store.
 * @param description   An optional description for the store. Max length 255.
 */
public record CreateStoreRequest(
        @NotBlank(message = "Store name cannot be blank.") @Size(min = 3, max = 100, message = "Store name must be between 3 and 100 characters.") String name,

        @NotBlank(message = "Subdomain cannot be blank.") @Pattern(regexp = "^[a-z0-9-]{3,50}$", message = "Subdomain must be lowercase alphanumeric with hyphens, and be between 3 and 50 characters long.") @Size(min = 3, max = 50, message = "Subdomain must be between 3 and 50 characters.") String subdomain,

        @NotBlank(message = "Email cannot be blank.") @Email(message = "Email should be valid.") @Size(max = 255, message = "Email cannot exceed 255 characters.") String email,

        @NotBlank(message = "Currency code cannot be blank.") @Size(min = 3, max = 3, message = "Currency code must be 3 characters long (e.g., USD).") String currencyCode,

        @NotBlank(message = "Default locale cannot be blank.") @Size(max = 10, message = "Default locale cannot exceed 10 characters (e.g., en-US).") String defaultLocale,

        StoreStatus status, // This can be null if it's optional and handled by
                            // the service

        @Size(max = 255, message = "Description cannot exceed 255 characters.") String description) {
}
