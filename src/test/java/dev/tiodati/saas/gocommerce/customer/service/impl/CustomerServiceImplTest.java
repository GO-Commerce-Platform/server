package dev.tiodati.saas.gocommerce.customer.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import dev.tiodati.saas.gocommerce.customer.dto.CustomerDto;
import dev.tiodati.saas.gocommerce.customer.entity.Customer;
import dev.tiodati.saas.gocommerce.customer.entity.CustomerStatus;
import dev.tiodati.saas.gocommerce.customer.repository.CustomerRepository;
import dev.tiodati.saas.gocommerce.customer.service.CustomerService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Integration tests for CustomerService implementation.
 * Tests the complete customer workflow using real service and database
 * interactions.
 */
@QuarkusTest
@DisplayName("CustomerServiceImpl Tests")
class CustomerServiceImplTest {

    @Inject
    private CustomerService customerService;

    @Inject
    private CustomerRepository customerRepository;

    private UUID storeId;
    private CreateCustomerDto createCustomerDto;

    @BeforeEach
    void setUp() {
        // Generate unique test data for isolation
        var uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        storeId = UUID.randomUUID();

        createCustomerDto = new CreateCustomerDto(
                "test-" + uniqueSuffix + "@example.com",
                "John",
                "Doe",
                "+1234567890",
                LocalDate.of(1990, 1, 1),
                Customer.Gender.MALE,
                "123 Main St",
                "Apt 4B",
                "New York",
                "NY",
                "10001",
                "US",
                false,
                "en");
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Clean up test data to maintain test isolation
        customerRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create Customer Tests")
    class CreateCustomerTests {

        @Test
        @DisplayName("Should create customer successfully")
        void shouldCreateCustomerSuccessfully() {
            // When - Real service call
            var result = customerService.createCustomer(storeId, createCustomerDto);

            // Then - Assert real results
            assertNotNull(result);
            assertNotNull(result.id());
            assertEquals(createCustomerDto.email(), result.email());
            assertEquals(createCustomerDto.firstName(), result.firstName());
            assertEquals(createCustomerDto.lastName(), result.lastName());
            assertEquals(CustomerStatus.ACTIVE, result.status());
            assertNotNull(result.createdAt());
        }

        @Test
        @DisplayName("Should normalize email to lowercase when creating customer")
        void shouldNormalizeEmailToLowercase() {
            // Given - DTO with uppercase email
            var upperCaseEmailDto = new CreateCustomerDto(
                    "TEST@EXAMPLE.COM",
                    "John",
                    "Doe",
                    null, null, null, null, null, null, null, null, null, null, null);

            // When - Real service call
            var result = customerService.createCustomer(storeId, upperCaseEmailDto);

            // Then - Assert email is normalized
            assertEquals("test@example.com", result.email());
        }

        @Test
        @DisplayName("Should set default values for optional fields")
        void shouldSetDefaultValuesForOptionalFields() {
            // Given - Minimal DTO
            var minimalDto = new CreateCustomerDto(
                    "minimal@example.com",
                    "John",
                    "Doe",
                    null, null, null, null, null, null, null, null, null, null, null);

            // When - Real service call
            var result = customerService.createCustomer(storeId, minimalDto);

            // Then - Assert defaults are set
            assertFalse(result.marketingEmailsOptIn());
            assertEquals("en", result.preferredLanguage());
        }
    }

    @Nested
    @DisplayName("List Customers Tests")
    class ListCustomersTests {

        @Test
        @DisplayName("Should list customers with pagination")
        void shouldListCustomersWithPagination() {
            // Given - Create real test data
            var customer = customerService.createCustomer(storeId, createCustomerDto);

            // When - Real service call
            var result = customerService.listCustomers(storeId, 0, 10, null);

            // Then - Assert real results
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(customer.email(), result.get(0).email());
        }

        @Test
        @DisplayName("Should list customers by status")
        void shouldListCustomersByStatus() {
            // Given - Create real test data
            var customer = customerService.createCustomer(storeId, createCustomerDto);

            // When - Real service call
            var result = customerService.listCustomers(storeId, 0, 10, CustomerStatus.ACTIVE);

            // Then - Assert real results
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(customer.email(), result.get(0).email());
        }

        @Test
        @DisplayName("Should return empty list when no customers found")
        void shouldReturnEmptyListWhenNoCustomersFound() {
            // When - Real service call without creating any customers
            var result = customerService.listCustomers(storeId, 0, 10, null);

            // Then - Assert real results
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find Customer Tests")
    class FindCustomerTests {

        @Test
        @DisplayName("Should find customer by ID")
        void shouldFindCustomerById() {
            // Given - Create real customer
            var customer = customerService.createCustomer(storeId, createCustomerDto);

            // When - Real service call
            var result = customerService.findCustomer(storeId, customer.id());

            // Then - Assert real results
            assertTrue(result.isPresent());
            assertEquals(customer.email(), result.get().email());
            assertEquals(customer.firstName(), result.get().firstName());
        }

        @Test
        @DisplayName("Should return empty when customer not found by ID")
        void shouldReturnEmptyWhenCustomerNotFoundById() {
            // Given - Non-existent customer ID
            var nonExistentId = UUID.randomUUID();

            // When - Real service call
            var result = customerService.findCustomer(storeId, nonExistentId);

            // Then - Assert empty result
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should find customer by email")
        void shouldFindCustomerByEmail() {
            // Given - Create real customer
            var customer = customerService.createCustomer(storeId, createCustomerDto);

            // When - Real service call
            var result = customerService.findCustomerByEmail(storeId, customer.email());

            // Then - Assert real results
            assertTrue(result.isPresent());
            assertEquals(customer.email(), result.get().email());
        }

        @Test
        @DisplayName("Should return empty when customer not found by email")
        void shouldReturnEmptyWhenCustomerNotFoundByEmail() {
            // Given - Non-existent email
            var nonExistentEmail = "notfound@example.com";

            // When - Real service call
            var result = customerService.findCustomerByEmail(storeId, nonExistentEmail);

            // Then - Assert empty result
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Update Customer Tests")
    class UpdateCustomerTests {

        @Test
        @DisplayName("Should update customer successfully")
        void shouldUpdateCustomerSuccessfully() {
            // Given - Create real customer
            var customer = customerService.createCustomer(storeId, createCustomerDto);
            var updateDto = new CustomerDto(
                    customer.id(),
                    "updated@example.com",
                    "Jane",
                    "Smith",
                    "+9876543210",
                    LocalDate.of(1985, 5, 15),
                    Customer.Gender.FEMALE,
                    "456 Oak Ave",
                    "Suite 2",
                    "Boston",
                    "MA",
                    "02101",
                    "US",
                    CustomerStatus.ACTIVE,
                    true,
                    true,
                    "es",
                    customer.createdAt(),
                    Instant.now());

            // When - Real service call
            var result = customerService.updateCustomer(storeId, updateDto);

            // Then - Assert real results
            assertTrue(result.isPresent());
            assertEquals("Jane", result.get().firstName());
            assertEquals("Smith", result.get().lastName());
            assertEquals("+9876543210", result.get().phone());
            assertEquals("es", result.get().preferredLanguage());
            assertTrue(result.get().marketingEmailsOptIn());
        }

        @Test
        @DisplayName("Should return empty when updating non-existent customer")
        void shouldReturnEmptyWhenUpdatingNonExistentCustomer() {
            // Given - DTO for non-existent customer
            var nonExistentId = UUID.randomUUID();
            var updateDto = new CustomerDto(
                    nonExistentId, "test@example.com", "John", "Doe", null, null, null,
                    null, null, null, null, null, null, CustomerStatus.ACTIVE,
                    false, false, "en", Instant.now(), Instant.now());

            // When - Real service call
            var result = customerService.updateCustomer(storeId, updateDto);

            // Then - Assert empty result
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Update Customer Status Tests")
    class UpdateCustomerStatusTests {

        @Test
        @DisplayName("Should update customer status successfully")
        void shouldUpdateCustomerStatusSuccessfully() {
            // Given - Create real customer
            var customer = customerService.createCustomer(storeId, createCustomerDto);
            var newStatus = CustomerStatus.SUSPENDED;

            // When - Real service call
            var result = customerService.updateCustomerStatus(storeId, customer.id(), newStatus);

            // Then - Assert real results
            assertTrue(result.isPresent());
            assertEquals(newStatus, result.get().status());
        }

        @Test
        @DisplayName("Should return empty when updating status of non-existent customer")
        void shouldReturnEmptyWhenUpdatingStatusOfNonExistentCustomer() {
            // Given - Non-existent customer ID
            var nonExistentId = UUID.randomUUID();
            var newStatus = CustomerStatus.SUSPENDED;

            // When - Real service call
            var result = customerService.updateCustomerStatus(storeId, nonExistentId, newStatus);

            // Then - Assert empty result
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Search Customers Tests")
    class SearchCustomersTests {

        @Test
        @DisplayName("Should search customers successfully")
        void shouldSearchCustomersSuccessfully() {
            // Given - Create real customer
            var customer = customerService.createCustomer(storeId, createCustomerDto);
            var searchTerm = "john";

            // When - Real service call
            var result = customerService.searchCustomers(storeId, searchTerm, 0, 10);

            // Then - Assert real results
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(customer.email(), result.get(0).email());
        }

        @Test
        @DisplayName("Should return empty list when no customers match search")
        void shouldReturnEmptyListWhenNoCustomersMatchSearch() {
            // Given - Create real customer
            customerService.createCustomer(storeId, createCustomerDto);
            var searchTerm = "nonexistent";

            // When - Real service call
            var result = customerService.searchCustomers(storeId, searchTerm, 0, 10);

            // Then - Assert empty result
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Count Customers Tests")
    class CountCustomersTests {

        @Test
        @DisplayName("Should count customers by status")
        void shouldCountCustomersByStatus() {
            // Given - Create real customers
            customerService.createCustomer(storeId, createCustomerDto);

            var secondCustomerDto = new CreateCustomerDto(
                    "second@example.com",
                    "Jane",
                    "Smith",
                    null, null, null, null, null, null, null, null, null, null, null);
            customerService.createCustomer(storeId, secondCustomerDto);

            // When - Real service call
            var result = customerService.countCustomersByStatus(storeId, CustomerStatus.ACTIVE);

            // Then - Assert real count
            assertEquals(2L, result);
        }

        @Test
        @DisplayName("Should return zero when no customers found for status")
        void shouldReturnZeroWhenNoCustomersFoundForStatus() {
            // Given - No customers created

            // When - Real service call
            var result = customerService.countCustomersByStatus(storeId, CustomerStatus.SUSPENDED);

            // Then - Assert zero count
            assertEquals(0L, result);
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.

// Copilot: This file may have been generated or refactored by GitHub Copilot.
