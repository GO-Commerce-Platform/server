package dev.tiodati.saas.gocommerce.customer.service;

import java.util.List;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.auth.dto.KeycloakUserCreateRequest;
import dev.tiodati.saas.gocommerce.auth.service.KeycloakAdminService;
import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import dev.tiodati.saas.gocommerce.customer.dto.CustomerDto;
import dev.tiodati.saas.gocommerce.customer.entity.CustomerStatus;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Service for managing customer registration and authentication integration.
 * This service handles the complete customer registration workflow including
 * Keycloak user creation, role assignment, and customer profile creation.
 */
@ApplicationScoped
public class CustomerRegistrationService {

    private final CustomerService customerService;
    private final KeycloakAdminService keycloakAdminService;

    @Inject
    public CustomerRegistrationService(
            CustomerService customerService,
            KeycloakAdminService keycloakAdminService) {
        this.customerService = customerService;
        this.keycloakAdminService = keycloakAdminService;
    }

    /**
     * Registers a new customer with integrated Keycloak user creation.
     * This method creates both a Keycloak user and a customer profile,
     * ensuring they are linked and properly configured.
     *
     * @param storeId            The store ID
     * @param customerDto        The customer registration data
     * @param password           The customer's password for Keycloak
     * @param assignDefaultRoles Whether to assign default customer roles
     * @return The created customer
     * @throws CustomerRegistrationException if registration fails
     */
    @Transactional
    public CustomerDto registerCustomerWithAuth(
            UUID storeId,
            CreateCustomerDto customerDto,
            String password,
            boolean assignDefaultRoles) {

        Log.infof("Starting customer registration for %s in store %s",
                customerDto.email(), storeId);

        try {
            // Step 1: Check if customer already exists
            var existingCustomer = customerService.findCustomerByEmail(storeId, customerDto.email());
            if (existingCustomer.isPresent()) {
                throw new CustomerRegistrationException(
                        "Customer with email " + customerDto.email() + " already exists");
            }

            // Step 2: Create Keycloak user
            String keycloakUserId = createKeycloakUser(customerDto, password);
            Log.infof("Created Keycloak user with ID: %s", keycloakUserId);

            // Step 3: Assign default customer roles if requested
            if (assignDefaultRoles) {
                assignCustomerRoles(keycloakUserId);
            }

            // Step 4: Create customer profile
            CustomerDto createdCustomer = customerService.createCustomer(storeId, customerDto);
            Log.infof("Created customer profile with ID: %s", createdCustomer.id());

            // TODO: Store the association between customer ID and Keycloak user ID
            // This might require extending the Customer entity with a keycloakUserId field

            Log.infof("Successfully completed customer registration for %s", customerDto.email());
            return createdCustomer;

        } catch (CustomerRegistrationException e) {
            // Re-throw our custom exceptions
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "Failed to register customer %s", customerDto.email());
            throw new CustomerRegistrationException(
                    "Failed to register customer: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a new customer with simplified workflow for self-registration.
     * Uses default settings for role assignment and email verification.
     *
     * @param storeId     The store ID
     * @param customerDto The customer registration data
     * @param password    The customer's password
     * @return The created customer
     */
    @Transactional
    public CustomerDto registerCustomer(
            UUID storeId,
            CreateCustomerDto customerDto,
            String password) {
        return registerCustomerWithAuth(storeId, customerDto, password, true);
    }

    /**
     * Creates a Keycloak user from customer registration data.
     *
     * @param customerDto The customer data
     * @param password    The user's password
     * @return The Keycloak user ID
     */
    private String createKeycloakUser(CreateCustomerDto customerDto, String password) {
        // Generate username from email (could be customized)
        String username = generateUsername(customerDto.email());

        KeycloakUserCreateRequest keycloakRequest = new KeycloakUserCreateRequest(
                username,
                customerDto.email(),
                customerDto.firstName(),
                customerDto.lastName(),
                password,
                false // Email verification will be handled separately
        );

        return keycloakAdminService.createUser(keycloakRequest);
    }

    /**
     * Assigns default customer roles to a Keycloak user.
     *
     * @param keycloakUserId The Keycloak user ID
     */
    private void assignCustomerRoles(String keycloakUserId) {
        try {
            // Assign basic customer roles
            List<String> customerRoles = List.of("CUSTOMER", "user");
            keycloakAdminService.assignRealmRolesToUser(keycloakUserId, customerRoles);

            Log.infof("Assigned customer roles to Keycloak user %s", keycloakUserId);
        } catch (Exception e) {
            Log.warnf(e, "Failed to assign roles to user %s, but registration will continue",
                    keycloakUserId);
            // Don't fail the entire registration if role assignment fails
        }
    }

    /**
     * Generates a username from an email address.
     * Could be enhanced with uniqueness checking and collision handling.
     *
     * @param email The email address
     * @return A username derived from the email
     */
    private String generateUsername(String email) {
        // Simple implementation: use the local part of the email
        String localPart = email.substring(0, email.indexOf('@'));

        // Clean up the username to meet typical requirements
        return localPart.toLowerCase()
                .replaceAll("[^a-z0-9._-]", "_");
    }

    /**
     * Validates customer registration data.
     *
     * @param customerDto The customer data to validate
     * @param password    The password to validate
     * @throws CustomerRegistrationException if validation fails
     */
    public void validateRegistrationData(CreateCustomerDto customerDto, String password) {
        if (customerDto == null) {
            throw new CustomerRegistrationException("Customer data is required");
        }

        if (customerDto.email() == null || customerDto.email().trim().isEmpty()) {
            throw new CustomerRegistrationException("Email is required");
        }

        if (customerDto.firstName() == null || customerDto.firstName().trim().isEmpty()) {
            throw new CustomerRegistrationException("First name is required");
        }

        if (customerDto.lastName() == null || customerDto.lastName().trim().isEmpty()) {
            throw new CustomerRegistrationException("Last name is required");
        }

        if (password == null || password.length() < 8) {
            throw new CustomerRegistrationException("Password must be at least 8 characters long");
        }

        // Add more validation rules as needed
        if (!isValidEmail(customerDto.email())) {
            throw new CustomerRegistrationException("Invalid email format");
        }
    }

    /**
     * Basic email validation.
     *
     * @param email The email to validate
     * @return true if email appears valid
     */
    private boolean isValidEmail(String email) {
        return email != null &&
                email.contains("@") &&
                email.indexOf("@") > 0 &&
                email.indexOf("@") < email.length() - 1;
    }

    /**
     * Links an existing customer with a Keycloak user.
     * Useful for migrating existing customers to use Keycloak authentication.
     *
     * @param storeId        The store ID
     * @param customerId     The customer ID
     * @param keycloakUserId The Keycloak user ID
     * @return The updated customer
     */
    @Transactional
    public CustomerDto linkCustomerWithKeycloak(
            UUID storeId,
            UUID customerId,
            String keycloakUserId) {

        Log.infof("Linking customer %s with Keycloak user %s", customerId, keycloakUserId);

        // TODO: Implement the linking logic
        // This would require extending the Customer entity to store keycloakUserId
        // For now, return the existing customer

        return customerService.findCustomer(storeId, customerId)
                .orElseThrow(() -> new CustomerRegistrationException("Customer not found"));
    }

    /**
     * Updates customer status and optionally enables/disables the associated
     * Keycloak user.
     *
     * @param storeId        The store ID
     * @param customerId     The customer ID
     * @param status         The new customer status
     * @param updateKeycloak Whether to update the Keycloak user status as well
     * @return The updated customer
     */
    @Transactional
    public CustomerDto updateCustomerStatus(
            UUID storeId,
            UUID customerId,
            CustomerStatus status,
            boolean updateKeycloak) {

        Log.infof("Updating customer %s status to %s", customerId, status);

        // Update customer status
        var updatedCustomer = customerService.updateCustomerStatus(storeId, customerId, status)
                .orElseThrow(() -> new CustomerRegistrationException("Customer not found"));

        // TODO: If updateKeycloak is true and we have keycloakUserId,
        // update the Keycloak user enabled status based on customer status

        if (updateKeycloak) {
            Log.infof("Keycloak user status update requested for customer %s", customerId);
            // Implementation would require keycloakUserId from customer entity
        }

        return updatedCustomer;
    }

    /**
     * Exception thrown when customer registration fails.
     */
    public static class CustomerRegistrationException extends RuntimeException {
        public CustomerRegistrationException(String message) {
            super(message);
        }

        public CustomerRegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
