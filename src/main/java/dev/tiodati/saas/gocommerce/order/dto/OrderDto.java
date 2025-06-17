package dev.tiodati.saas.gocommerce.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for OrderHeader information.
 *
 * @param id                    Order unique identifier
 * @param orderNumber           Human-readable order number
 * @param customerId            Customer ID who placed the order
 * @param statusId              Current order status ID
 * @param statusName            Current order status name
 * @param subtotal              Subtotal before taxes and shipping
 * @param taxAmount             Total tax amount
 * @param shippingAmount        Total shipping cost
 * @param discountAmount        Total discount amount
 * @param totalAmount           Final total amount
 * @param currencyCode          Currency code (ISO 4217)
 * @param locale                Locale for the order
 * @param shippingFirstName     Shipping address first name
 * @param shippingLastName      Shipping address last name
 * @param shippingAddressLine1  Shipping address line 1
 * @param shippingAddressLine2  Shipping address line 2
 * @param shippingCity          Shipping address city
 * @param shippingStateProvince Shipping address state/province
 * @param shippingPostalCode    Shipping address postal code
 * @param shippingCountry       Shipping address country
 * @param shippingPhone         Shipping address phone
 * @param billingFirstName      Billing address first name
 * @param billingLastName       Billing address last name
 * @param billingAddressLine1   Billing address line 1
 * @param billingAddressLine2   Billing address line 2
 * @param billingCity           Billing address city
 * @param billingStateProvince  Billing address state/province
 * @param billingPostalCode     Billing address postal code
 * @param billingCountry        Billing address country
 * @param billingPhone          Billing address phone
 * @param orderDate             When the order was placed
 * @param shippedDate           When the order was shipped
 * @param deliveredDate         When the order was delivered
 * @param notes                 Additional notes for the order
 * @param createdAt             Timestamp when the order was created
 * @param updatedAt             Timestamp when the order was last updated
 * @param version               Version for optimistic locking
 * @param items                 List of order items
 */
public record OrderDto(
        UUID id,
        String orderNumber,
        UUID customerId,
        String statusId,
        String statusName,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal shippingAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String currencyCode,
        String locale,
        String shippingFirstName,
        String shippingLastName,
        String shippingAddressLine1,
        String shippingAddressLine2,
        String shippingCity,
        String shippingStateProvince,
        String shippingPostalCode,
        String shippingCountry,
        String shippingPhone,
        String billingFirstName,
        String billingLastName,
        String billingAddressLine1,
        String billingAddressLine2,
        String billingCity,
        String billingStateProvince,
        String billingPostalCode,
        String billingCountry,
        String billingPhone,
        Instant orderDate,
        Instant shippedDate,
        Instant deliveredDate,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        Integer version,
        List<OrderItemDto> items) {
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
