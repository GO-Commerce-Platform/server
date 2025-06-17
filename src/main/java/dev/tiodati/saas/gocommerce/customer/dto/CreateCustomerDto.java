package dev.tiodati.saas.gocommerce.customer.dto;

import java.time.LocalDate;

import dev.tiodati.saas.gocommerce.customer.entity.Customer;

/**
 * Data Transfer Object for creating a new Customer.
 *
 * @param email                Customer's email address (required)
 * @param firstName            Customer's first name (required)
 * @param lastName             Customer's last name (required)
 * @param phone                Customer's phone number (optional)
 * @param dateOfBirth          Customer's date of birth (optional)
 * @param gender               Customer's gender (optional)
 * @param addressLine1         Primary address line (optional)
 * @param addressLine2         Secondary address line (optional)
 * @param city                 City name (optional)
 * @param stateProvince        State or province name (optional)
 * @param postalCode           Postal/ZIP code (optional)
 * @param country              Country code (ISO 3166-1 alpha-2) (optional)
 * @param marketingEmailsOptIn Whether customer opts in to marketing emails
 *                             (optional, defaults to false)
 * @param preferredLanguage    Customer's preferred language (ISO 639-1)
 *                             (optional, defaults to "en")
 */
public record CreateCustomerDto(
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
        Boolean marketingEmailsOptIn,
        String preferredLanguage) {
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
