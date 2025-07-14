package dev.tiodati.saas.gocommerce.customer.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import dev.tiodati.saas.gocommerce.customer.dto.CustomerDto;
import dev.tiodati.saas.gocommerce.customer.entity.Customer;
import dev.tiodati.saas.gocommerce.customer.entity.CustomerStatus;
import dev.tiodati.saas.gocommerce.customer.repository.CustomerRepository;
import dev.tiodati.saas.gocommerce.shared.interceptor.TenantAware;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of CustomerService providing store-specific customer
 * management functionality.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    /**
     * Repository for customer data access operations.
     */
    private final CustomerRepository customerRepository;

    @Override
    @TenantAware
    public List<CustomerDto> listCustomers(UUID storeId, int page, int size, CustomerStatus status) {
        Log.infof("Listing customers for store %s (page=%d, size=%d, status=%s)",
                storeId, page, size, status);

        List<Customer> customers;
        var pageable = Page.of(page, size);

        if (status != null) {
            customers = customerRepository.findByStatus(status, pageable);
        } else {
            customers = customerRepository.findActiveCustomers(pageable);
        }

        return customers.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @TenantAware
    public Optional<CustomerDto> findCustomer(UUID storeId, UUID customerId) {
        Log.infof("Finding customer %s for store %s", customerId, storeId);
        return customerRepository.findByIdOptional(customerId)
                .map(this::mapToDto);
    }

    @Override
    @TenantAware
    public Optional<CustomerDto> findCustomerByEmail(UUID storeId, String email) {
        Log.infof("Finding customer by email %s for store %s", email, storeId);
        return customerRepository.findByEmail(email)
                .map(this::mapToDto);
    }

    @Override
    @TenantAware
    public CustomerDto createCustomer(UUID storeId, CreateCustomerDto customerDto) {
        Log.infof("Creating customer for store %s: %s", storeId, customerDto.email());

        var customer = Customer.builder()
                .email(customerDto.email().toLowerCase()) // Normalize email to lowercase
                .firstName(customerDto.firstName())
                .lastName(customerDto.lastName())
                .phone(customerDto.phone())
                .dateOfBirth(customerDto.dateOfBirth())
                .gender(customerDto.gender())
                .addressLine1(customerDto.addressLine1())
                .addressLine2(customerDto.addressLine2())
                .city(customerDto.city())
                .stateProvince(customerDto.stateProvince())
                .postalCode(customerDto.postalCode())
                .country(customerDto.country())
                .status(CustomerStatus.ACTIVE)
                .emailVerified(false)
                .marketingEmailsOptIn(
                        customerDto.marketingEmailsOptIn() != null ? customerDto.marketingEmailsOptIn() : false)
                .preferredLanguage(customerDto.preferredLanguage() != null ? customerDto.preferredLanguage() : "en")
                .build();

        customerRepository.persist(customer);

        return mapToDto(customer);
    }

    @Override
    @TenantAware
    public Optional<CustomerDto> updateCustomer(UUID storeId, CustomerDto customerDto) {
        Log.infof("Updating customer %s for store %s", customerDto.id(), storeId);

        return customerRepository.findByIdOptional(customerDto.id())
            .map(customer -> {
                customer.setFirstName(customerDto.firstName());
                customer.setLastName(customerDto.lastName());
                customer.setPhone(customerDto.phone());
                customer.setDateOfBirth(customerDto.dateOfBirth());
                customer.setGender(customerDto.gender());
                customer.setAddressLine1(customerDto.addressLine1());
                customer.setAddressLine2(customerDto.addressLine2());
                customer.setCity(customerDto.city());
                customer.setStateProvince(customerDto.stateProvince());
                customer.setPostalCode(customerDto.postalCode());
                customer.setCountry(customerDto.country());

                if (customerDto.marketingEmailsOptIn() != null) {
                    customer.setMarketingEmailsOptIn(customerDto.marketingEmailsOptIn());
                }

                if (customerDto.preferredLanguage() != null) {
                    customer.setPreferredLanguage(customerDto.preferredLanguage());
                }

                return mapToDto(customer);
            });
    }

    @Override
    @TenantAware
    public Optional<CustomerDto> updateCustomerStatus(UUID storeId, UUID customerId, CustomerStatus status) {
        Log.infof("Updating customer %s status to %s for store %s", customerId, status, storeId);

        return customerRepository.findByIdOptional(customerId)
            .map(customer -> {
                customer.setStatus(status);
                return mapToDto(customer);
            });
    }

    @Override
    @TenantAware
    public List<CustomerDto> searchCustomers(UUID storeId, String searchTerm, int page, int size) {
        Log.infof("Searching customers for store %s with term: %s (page=%d, size=%d)",
                storeId, searchTerm, page, size);

        var pageable = Page.of(page, size);
        var customers = customerRepository.searchCustomers(searchTerm, pageable);

        return customers.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @TenantAware
    public long countCustomersByStatus(UUID storeId, CustomerStatus status) {
        Log.infof("Counting customers by status %s for store %s", status, storeId);
        return status != null ? customerRepository.countByStatus(status) : customerRepository.count();
    }

    /**
     * Maps a Customer entity to CustomerDto.
     *
     * @param customer the Customer entity to map
     * @return the corresponding CustomerDto
     */
    private CustomerDto mapToDto(Customer customer) {
        return new CustomerDto(
                customer.getId(),
                customer.getEmail(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getPhone(),
                customer.getDateOfBirth(),
                customer.getGender(),
                customer.getAddressLine1(),
                customer.getAddressLine2(),
                customer.getCity(),
                customer.getStateProvince(),
                customer.getPostalCode(),
                customer.getCountry(),
                customer.getStatus(),
                customer.getEmailVerified(),
                customer.getMarketingEmailsOptIn(),
                customer.getPreferredLanguage(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}
