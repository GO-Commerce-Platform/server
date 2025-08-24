package dev.tiodati.saas.gocommerce.product.dto;

import dev.tiodati.saas.gocommerce.product.entity.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data transfer object containing product availability information.
 * Used to provide quick availability checks without loading full product details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAvailability {

    /**
     * Product identifier.
     */
    private UUID productId;

    /**
     * Product name.
     */
    private String productName;

    /**
     * Whether the product is available for purchase.
     * Based on status being ACTIVE and stock availability.
     */
    private boolean available;

    /**
     * Current product status.
     */
    private ProductStatus status;

    /**
     * Whether inventory tracking is enabled.
     */
    private Boolean trackInventory;

    /**
     * Current stock quantity.
     * Only relevant if trackInventory is true.
     */
    private Integer stockQuantity;

    /**
     * Low stock threshold.
     */
    private Integer lowStockThreshold;

    /**
     * Whether the product has low stock.
     * Only relevant if trackInventory is true.
     */
    private boolean lowStock;

    /**
     * Whether the product is out of stock.
     * Only relevant if trackInventory is true.
     */
    private boolean outOfStock;

    /**
     * Availability message explaining the current state.
     */
    private String availabilityMessage;
}
