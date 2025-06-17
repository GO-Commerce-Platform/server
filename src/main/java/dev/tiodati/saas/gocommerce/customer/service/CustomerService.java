package dev.tiodati.saas.gocommerce.customer.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import dev.tiodati.saas.gocommerce.customer.dto.CustomerDto;
import dev.tiodati.saas.gocommerce.customer.entity.CustomerStatus;

/**
 * Service interface for customer management operations.
 * Provides methods for creating, retrieving, updating, and managing customers.
 */
public interface CustomerService {

    /**
     * List customers for a store with pagination and optional status filter.
     *
     * @param storeId The store ID
     * @param page    Page number (0-based)
     * @param size    Page size
     * @param status  Optional status filter
     * @return List of customer DTOs
     */
    List<CustomerDto> listCustomers(UUID storeId, int page, int size, CustomerStatus status);

    /**
     * Find a customer by ID.
     *
     * @param storeId    The store ID
     * @param customerId The customer ID
     * @return Optional containing the customer if found
     */
    Optional<CustomerDto> findCustomer(UUID storeId, UUID customerId);

    /**
     * Find a customer by email address.
     *
     * @param storeId The store ID
     * @param email   The customer email
     * @return Optional containing the customer if found
     */
    Optional<CustomerDto> findCustomerByEmail(UUID storeId, String email);

    /**
     * Create a new customer.
     *
     * @param storeId     The store ID
     * @param customerDto Customer data
     * @return The created customer
     */
    CustomerDto createCustomer(UUID storeId, CreateCustomerDto customerDto);

    /**
     * Update an existing customer.
     *
     * @param storeId     The store ID
     * @param customerDto Updated customer data
     * @return Optional containing the updated customer if found
     */
    Optional<CustomerDto> updateCustomer(UUID storeId, CustomerDto customerDto);

    /**
     * Update customer status.
     *
     * @param storeId    The store ID
     * @param customerId The customer ID
     * @param status     The new status
     * @return Optional containing the updated customer if found
     */
    Optional<CustomerDto> updateCustomerStatus(UUID storeId, UUID customerId, CustomerStatus status);

    /**
     * Search customers by name or email.
     *
     * @param storeId    The store ID
     * @param searchTerm The search term
     * @param page       Page number (0-based)
     * @param size       Page size
     * @return List of matching customers
     */
    List<CustomerDto> searchCustomers(UUID storeId, String searchTerm, int page, int size);

    /**
     * Get count of customers by status.
     *
     * @param storeId The store ID
     * @param status  The customer status
     * @return Count of customers with the specified status
     */
    long countCustomersByStatus(UUID storeId, CustomerStatus status);
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
