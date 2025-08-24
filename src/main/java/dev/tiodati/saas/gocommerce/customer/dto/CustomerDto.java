package dev.tiodati.saas.gocommerce.customer.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.customer.entity.Customer;
import dev.tiodati.saas.gocommerce.customer.entity.CustomerStatus;

/**
 * Data Transfer Object for Customer information.
 *
 * @param id                   Customer's unique identifier
 * @param email                Customer's email address
 * @param firstName            Customer's first name
 * @param lastName             Customer's last name
 * @param phone                Customer's phone number
 * @param dateOfBirth          Customer's date of birth
 * @param gender               Customer's gender
 * @param addressLine1         Primary address line
 * @param addressLine2         Secondary address line
 * @param city                 City name
 * @param stateProvince        State or province name
 * @param postalCode           Postal/ZIP code
 * @param country              Country code (ISO 3166-1 alpha-2)
 * @param status               Customer account status
 * @param emailVerified        Whether the customer's email is verified
 * @param marketingEmailsOptIn Whether customer opted in to marketing emails
 * @param preferredLanguage    Customer's preferred language (ISO 639-1)
 * @param keycloakUserId       Associated Keycloak user ID
 * @param createdAt            Timestamp when the customer was created
 * @param updatedAt            Timestamp when the customer was last updated
 */
public record CustomerDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        LocalDate dateOfBirth,
        Customer.Gender gender,
        String addressLine1,
        String addressLine2,
        String city,
        String stateProvince,
        String postalCode,
        String country,
        CustomerStatus status,
        Boolean emailVerified,
        Boolean marketingEmailsOptIn,
        String preferredLanguage,
        String keycloakUserId,
        Instant createdAt,
        Instant updatedAt) {
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
