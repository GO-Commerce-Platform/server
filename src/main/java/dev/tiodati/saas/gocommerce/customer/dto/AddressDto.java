package dev.tiodati.saas.gocommerce.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for customer address information.
 * Supports both billing and shipping addresses with comprehensive validation.
 *
 * @param addressLine1  Primary address line (required)
 * @param addressLine2  Secondary address line (optional)
 * @param city          City name (required)
 * @param stateProvince State or province (required)
 * @param postalCode    Postal/ZIP code (required)
 * @param country       Country code (ISO 3166-1 alpha-2) (required)
 * @param addressType   Type of address (billing, shipping, etc.)
 * @param isDefault     Whether this is the default address of its type
 * @param firstName     First name for address (optional)
 * @param lastName      Last name for address (optional)
 * @param phone         Phone number for address (optional)
 * @param company       Company name (optional)
 */
public record AddressDto(
        @NotBlank(message = "Address line 1 is required") String addressLine1,

        String addressLine2,

        @NotBlank(message = "City is required") @Size(max = 100, message = "City cannot exceed 100 characters") String city,

        @NotBlank(message = "State/Province is required") @Size(max = 100, message = "State/Province cannot exceed 100 characters") String stateProvince,

        @NotBlank(message = "Postal code is required") @Size(max = 20, message = "Postal code cannot exceed 20 characters") String postalCode,

        @NotBlank(message = "Country is required") @Size(min = 2, max = 2, message = "Country must be a 2-character ISO code") String country,

        AddressType addressType,

        Boolean isDefault,

        @Size(max = 100, message = "First name cannot exceed 100 characters") String firstName,

        @Size(max = 100, message = "Last name cannot exceed 100 characters") String lastName,

        @Size(max = 20, message = "Phone cannot exceed 20 characters") String phone,

        @Size(max = 100, message = "Company cannot exceed 100 characters") String company) {

    /**
     * Enumeration for address types.
     */
    public enum AddressType {
        /** Billing address. */
        BILLING,
        /** Shipping address. */
        SHIPPING,
        /** General address. */
        GENERAL
    }

    /**
     * Creates a billing address.
     */
    public static AddressDto billing(
            String addressLine1,
            String addressLine2,
            String city,
            String stateProvince,
            String postalCode,
            String country) {
        return new AddressDto(
                addressLine1, addressLine2, city, stateProvince, postalCode, country,
                AddressType.BILLING, false, null, null, null, null);
    }

    /**
     * Creates a shipping address.
     */
    public static AddressDto shipping(
            String addressLine1,
            String addressLine2,
            String city,
            String stateProvince,
            String postalCode,
            String country) {
        return new AddressDto(
                addressLine1, addressLine2, city, stateProvince, postalCode, country,
                AddressType.SHIPPING, false, null, null, null, null);
    }

    /**
     * Creates a general address.
     */
    public static AddressDto general(
            String addressLine1,
            String addressLine2,
            String city,
            String stateProvince,
            String postalCode,
            String country) {
        return new AddressDto(
                addressLine1, addressLine2, city, stateProvince, postalCode, country,
                AddressType.GENERAL, false, null, null, null, null);
    }

    /**
     * Returns a copy of this address with the specified type.
     */
    public AddressDto withType(AddressType newType) {
        return new AddressDto(
                addressLine1, addressLine2, city, stateProvince, postalCode, country,
                newType, isDefault, firstName, lastName, phone, company);
    }

    /**
     * Returns a copy of this address marked as default.
     */
    public AddressDto asDefault() {
        return new AddressDto(
                addressLine1, addressLine2, city, stateProvince, postalCode, country,
                addressType, true, firstName, lastName, phone, company);
    }

    /**
     * Returns a copy of this address with contact information.
     */
    public AddressDto withContact(String firstName, String lastName, String phone) {
        return new AddressDto(
                addressLine1, addressLine2, city, stateProvince, postalCode, country,
                addressType, isDefault, firstName, lastName, phone, company);
    }

    /**
     * Returns a formatted single-line address string.
     */
    public String toFormattedString() {
        var sb = new StringBuilder();
        sb.append(addressLine1);
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            sb.append(", ").append(addressLine2);
        }
        sb.append(", ").append(city);
        sb.append(", ").append(stateProvince);
        sb.append(" ").append(postalCode);
        sb.append(", ").append(country);
        return sb.toString();
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
