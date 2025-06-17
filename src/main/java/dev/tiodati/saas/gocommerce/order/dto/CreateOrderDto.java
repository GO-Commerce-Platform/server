package dev.tiodati.saas.gocommerce.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating a new order.
 *
 * @param customerId            Customer ID who is placing the order (required)
 * @param shippingFirstName     Shipping address first name (required)
 * @param shippingLastName      Shipping address last name (required)
 * @param shippingAddressLine1  Shipping address line 1 (required)
 * @param shippingAddressLine2  Shipping address line 2 (optional)
 * @param shippingCity          Shipping address city (required)
 * @param shippingStateProvince Shipping address state/province (required)
 * @param shippingPostalCode    Shipping address postal code (required)
 * @param shippingCountry       Shipping address country (required)
 * @param shippingPhone         Shipping address phone (optional)
 * @param billingFirstName      Billing address first name (required)
 * @param billingLastName       Billing address last name (required)
 * @param billingAddressLine1   Billing address line 1 (required)
 * @param billingAddressLine2   Billing address line 2 (optional)
 * @param billingCity           Billing address city (required)
 * @param billingStateProvince  Billing address state/province (required)
 * @param billingPostalCode     Billing address postal code (required)
 * @param billingCountry        Billing address country (required)
 * @param billingPhone          Billing address phone (optional)
 * @param notes                 Additional notes for the order (optional)
 * @param items                 List of order items (required, non-empty)
 */
public record CreateOrderDto(
        @NotNull(message = "Customer ID is required") UUID customerId,

        @NotBlank(message = "Shipping first name is required") @Size(max = 100, message = "Shipping first name cannot exceed 100 characters") String shippingFirstName,

        @NotBlank(message = "Shipping last name is required") @Size(max = 100, message = "Shipping last name cannot exceed 100 characters") String shippingLastName,

        @NotBlank(message = "Shipping address line 1 is required") String shippingAddressLine1,

        String shippingAddressLine2,

        @NotBlank(message = "Shipping city is required") @Size(max = 100, message = "Shipping city cannot exceed 100 characters") String shippingCity,

        @NotBlank(message = "Shipping state/province is required") @Size(max = 100, message = "Shipping state/province cannot exceed 100 characters") String shippingStateProvince,

        @NotBlank(message = "Shipping postal code is required") @Size(max = 20, message = "Shipping postal code cannot exceed 20 characters") String shippingPostalCode,

        @NotBlank(message = "Shipping country is required") @Size(max = 2, message = "Shipping country must be a 2-character ISO code") String shippingCountry,

        @Size(max = 20, message = "Shipping phone cannot exceed 20 characters") String shippingPhone,

        @NotBlank(message = "Billing first name is required") @Size(max = 100, message = "Billing first name cannot exceed 100 characters") String billingFirstName,

        @NotBlank(message = "Billing last name is required") @Size(max = 100, message = "Billing last name cannot exceed 100 characters") String billingLastName,

        @NotBlank(message = "Billing address line 1 is required") String billingAddressLine1,

        String billingAddressLine2,

        @NotBlank(message = "Billing city is required") @Size(max = 100, message = "Billing city cannot exceed 100 characters") String billingCity,

        @NotBlank(message = "Billing state/province is required") @Size(max = 100, message = "Billing state/province cannot exceed 100 characters") String billingStateProvince,

        @NotBlank(message = "Billing postal code is required") @Size(max = 20, message = "Billing postal code cannot exceed 20 characters") String billingPostalCode,

        @NotBlank(message = "Billing country is required") @Size(max = 2, message = "Billing country must be a 2-character ISO code") String billingCountry,

        @Size(max = 20, message = "Billing phone cannot exceed 20 characters") String billingPhone,

        String notes,

        @NotEmpty(message = "Order must have at least one item") List<CreateOrderItemDto> items) {

    /**
     * Data Transfer Object for creating order items.
     *
     * @param productId Product ID (required)
     * @param quantity  Quantity to order (required, positive)
     * @param unitPrice Unit price (required, positive)
     */
    public record CreateOrderItemDto(
            @NotNull(message = "Product ID is required") UUID productId,

            @NotNull(message = "Quantity is required") @DecimalMin(value = "1", message = "Quantity must be at least 1") Integer quantity,

            @NotNull(message = "Unit price is required") @DecimalMin(value = "0.01", message = "Unit price must be greater than zero") BigDecimal unitPrice) {
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
