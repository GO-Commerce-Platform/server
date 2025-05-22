package dev.tiodati.saas.gocommerce.platform.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStoreRequest(
    @NotBlank(message = "Store name is required")
    @Size(min = 3, max = 100, message = "Store name must be between 3 and 100 characters")
    String name,
    
    @NotBlank(message = "Subdomain is required")
    @Pattern(regexp = "^[a-z0-9-]{3,50}$", message = "Subdomain must contain only lowercase letters, numbers, and hyphens (3-50 characters)")
    String subdomain,
    
    @Valid
    AdminUserDetails adminUser
) {}
