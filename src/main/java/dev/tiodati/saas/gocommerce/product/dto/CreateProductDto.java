package dev.tiodati.saas.gocommerce.product.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating a new product
 */
public record CreateProductDto(
    @NotBlank(message = "SKU is required")
    @Size(min = 3, max = 50, message = "SKU must be between 3 and 50 characters")
    String sku,
    
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    String name,
    
    @Size(max = 4000, message = "Description cannot exceed 4000 characters")
    String description,
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    BigDecimal price,
    
    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    BigDecimal cost,
    
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    int stockQuantity,
    
    boolean isActive,
    
    UUID categoryId,
    
    Map<String, String> attributes
) {}