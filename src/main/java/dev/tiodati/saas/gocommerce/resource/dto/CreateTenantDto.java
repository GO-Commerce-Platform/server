package dev.tiodati.saas.gocommerce.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantDto {
    @NotBlank(message = "Tenant key is required")
    @Pattern(regexp = "^[a-z0-9-]{3,50}$", message = "Tenant key must contain only lowercase letters, numbers, and hyphens, and be between 3-50 characters")
    private String tenantKey;
    
    @NotBlank(message = "Tenant name is required")
    @Size(min = 3, max = 255, message = "Tenant name must be between 3-255 characters")
    private String name;
    
    @NotBlank(message = "Subdomain is required")
    @Pattern(regexp = "^[a-z0-9-]{3,100}$", message = "Subdomain must contain only lowercase letters, numbers, and hyphens, and be between 3-100 characters")
    private String subdomain;
    
    private String billingPlan;
    
    private String settings;
    
    // Admin details
    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be valid")
    private String adminEmail;
    
    @NotBlank(message = "Admin password is required")
    @Size(min = 8, message = "Admin password must be at least 8 characters")
    private String adminPassword;
    
    @NotBlank(message = "Admin first name is required")
    private String adminFirstName;
    
    @NotBlank(message = "Admin last name is required")
    private String adminLastName;
}