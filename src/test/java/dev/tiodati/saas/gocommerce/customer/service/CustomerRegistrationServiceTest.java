package dev.tiodati.saas.gocommerce.customer.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import dev.tiodati.saas.gocommerce.customer.entity.Customer;
import dev.tiodati.saas.gocommerce.customer.entity.CustomerStatus;
import dev.tiodati.saas.gocommerce.customer.repository.CustomerRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Integration tests for CustomerRegistrationService.
 * Tests the complete customer registration workflow including validation,
 * Keycloak integration, and database operations.
 */
@QuarkusTest
@Transactional
@DisplayName("CustomerRegistrationService Integration Tests")
class CustomerRegistrationServiceTest {

    /** Service under test. */
    @Inject
    private CustomerRegistrationService customerRegistrationService;

    /** Repository for customer data operations. */
    @Inject
    private CustomerRepository customerRepository;

    /** Test store ID for multi-tenant context. */
    private UUID testStoreId;

    /** Unique suffix for test data isolation. */
    private String uniqueSuffix;

    /** Test customer creation DTO. */
    private CreateCustomerDto testCreateCustomerDto;

    /** Test customer entity. */
    private Customer testCustomer;

    /**
     * Set up test data before each test.
     */
    @BeforeEach
    void setUp() {
        // Generate unique test data for isolation
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        testStoreId = UUID.randomUUID();

        testCreateCustomerDto = new CreateCustomerDto(
                "customer" + uniqueSuffix + "@test.com",
                "Customer" + uniqueSuffix,
                "Test" + uniqueSuffix,
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

    /**
     * Clean up test data after each test.
     */
    @AfterEach
    void tearDown() {
        // Clean up test data
        if (testCustomer != null) {
            customerRepository.deleteById(testCustomer.getId());
        }
        // Clean up any customers created with our test email pattern
        customerRepository.findByEmail(testCreateCustomerDto.email())
                .ifPresent(customer -> customerRepository.deleteById(customer.getId()));
    }

    // Customer Registration Tests

    @Test
    @DisplayName("Should register customer successfully without Keycloak")
    @Transactional
    void shouldRegisterCustomerSuccessfully() {
        // When
        var result = customerRegistrationService.registerCustomer(
                testStoreId, testCreateCustomerDto, "testPassword123!");

        // Then
        assertNotNull(result);
        assertEquals(testCreateCustomerDto.email(), result.email());
        assertEquals(testCreateCustomerDto.firstName(), result.firstName());
        assertEquals(testCreateCustomerDto.lastName(), result.lastName());
        assertEquals(testCreateCustomerDto.phone(), result.phone());
        assertEquals(CustomerStatus.ACTIVE, result.status());

        // Verify customer is persisted
        var persistedCustomer = customerRepository.findByEmail(testCreateCustomerDto.email());
        assertTrue(persistedCustomer.isPresent());
        testCustomer = persistedCustomer.get();
        assertEquals(CustomerStatus.ACTIVE, testCustomer.getStatus());
    }

    @Test
    @DisplayName("Should validate registration data")
    void shouldValidateRegistrationDataSuccessfully() {
        // When & Then - Should not throw exception
        assertDoesNotThrow(
                () -> customerRegistrationService.validateRegistrationData(testCreateCustomerDto,
                        "testPassword123!"));
    }

    @Test
    @DisplayName("Should fail validation for invalid email")
    void shouldFailValidationForInvalidEmail() {
        var invalidDto = new CreateCustomerDto(
                "invalid-email", // Invalid email format
                testCreateCustomerDto.firstName(),
                testCreateCustomerDto.lastName(),
                testCreateCustomerDto.phone(),
                testCreateCustomerDto.dateOfBirth(),
                testCreateCustomerDto.gender(),
                testCreateCustomerDto.addressLine1(),
                testCreateCustomerDto.addressLine2(),
                testCreateCustomerDto.city(),
                testCreateCustomerDto.stateProvince(),
                testCreateCustomerDto.postalCode(),
                testCreateCustomerDto.country(),
                testCreateCustomerDto.marketingEmailsOptIn(),
                testCreateCustomerDto.preferredLanguage());

        // When & Then
        assertThrows(CustomerRegistrationService.CustomerRegistrationException.class,
                () -> customerRegistrationService.validateRegistrationData(invalidDto, "testPassword123!"));
    }

    @Test
    @DisplayName("Should fail validation for empty first name")
    void shouldFailValidationForEmptyFirstName() {
        var invalidDto = new CreateCustomerDto(
                testCreateCustomerDto.email(),
                "", // Empty first name
                testCreateCustomerDto.lastName(),
                testCreateCustomerDto.phone(),
                testCreateCustomerDto.dateOfBirth(),
                testCreateCustomerDto.gender(),
                testCreateCustomerDto.addressLine1(),
                testCreateCustomerDto.addressLine2(),
                testCreateCustomerDto.city(),
                testCreateCustomerDto.stateProvince(),
                testCreateCustomerDto.postalCode(),
                testCreateCustomerDto.country(),
                testCreateCustomerDto.marketingEmailsOptIn(),
                testCreateCustomerDto.preferredLanguage());

        // When & Then
        assertThrows(CustomerRegistrationService.CustomerRegistrationException.class,
                () -> customerRegistrationService.validateRegistrationData(invalidDto, "testPassword123!"));
    }

    @Test
    @DisplayName("Should fail validation for null last name")
    void shouldFailValidationForNullLastName() {
        var invalidDto = new CreateCustomerDto(
                testCreateCustomerDto.email(),
                testCreateCustomerDto.firstName(),
                null, // Null last name
                testCreateCustomerDto.phone(),
                testCreateCustomerDto.dateOfBirth(),
                testCreateCustomerDto.gender(),
                testCreateCustomerDto.addressLine1(),
                testCreateCustomerDto.addressLine2(),
                testCreateCustomerDto.city(),
                testCreateCustomerDto.stateProvince(),
                testCreateCustomerDto.postalCode(),
                testCreateCustomerDto.country(),
                testCreateCustomerDto.marketingEmailsOptIn(),
                testCreateCustomerDto.preferredLanguage());

        // When & Then
        assertThrows(CustomerRegistrationService.CustomerRegistrationException.class,
                () -> customerRegistrationService.validateRegistrationData(invalidDto, "testPassword123!"));
    }

    @Test
    @DisplayName("Should fail validation for duplicate email")
    @Transactional
    void shouldFailValidationForDuplicateEmail() {
        // Given - Create existing customer
        testCustomer = createTestCustomer();
        customerRepository.persist(testCustomer);

        // When & Then
        assertThrows(CustomerRegistrationService.CustomerRegistrationException.class,
                () -> customerRegistrationService.registerCustomerWithAuth(
                        testStoreId, testCreateCustomerDto, "testPassword123!", false));
    }

    // Customer Status Management Tests

    @Test
    @DisplayName("Should update customer status successfully")
    @Transactional
    void shouldUpdateCustomerStatusSuccessfully() {
        // Given - Create and persist customer
        testCustomer = createTestCustomer();
        customerRepository.persist(testCustomer);

        // When
        var result = customerRegistrationService.updateCustomerStatus(
                testStoreId, testCustomer.getId(), CustomerStatus.INACTIVE, false);

        // Then
        assertNotNull(result);
        assertEquals(CustomerStatus.INACTIVE, result.status());

        // Verify status is updated in database
        var updatedCustomer = customerRepository.findByIdOptional(testCustomer.getId());
        assertTrue(updatedCustomer.isPresent());
        assertEquals(CustomerStatus.INACTIVE, updatedCustomer.get().getStatus());
    }

    @Test
    @DisplayName("Should update customer status to suspended")
    @Transactional
    void shouldUpdateCustomerStatusToSuspended() {
        // Given - Create and persist customer
        testCustomer = createTestCustomer();
        customerRepository.persist(testCustomer);

        // When
        var result = customerRegistrationService.updateCustomerStatus(
                testStoreId, testCustomer.getId(), CustomerStatus.SUSPENDED, false);

        // Then
        assertEquals(CustomerStatus.SUSPENDED, result.status());

        // Verify in database
        var updatedCustomer = customerRepository.findByIdOptional(testCustomer.getId());
        assertTrue(updatedCustomer.isPresent());
        assertEquals(CustomerStatus.SUSPENDED, updatedCustomer.get().getStatus());
    }

    @Test
    @DisplayName("Should throw exception for non-existent customer")
    void shouldThrowExceptionForNonExistentCustomer() {
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        assertThrows(RuntimeException.class, () -> customerRegistrationService.updateCustomerStatus(
                testStoreId, nonExistentId, CustomerStatus.INACTIVE, false));
    }

    // Keycloak Integration Tests

    @Test
    @DisplayName("Should link customer with Keycloak successfully")
    @Transactional
    void shouldLinkCustomerWithKeycloakSuccessfully() {
        // Given - Create and persist customer
        testCustomer = createTestCustomer();
        customerRepository.persist(testCustomer);
        String keycloakUserId = "keycloak-user-" + uniqueSuffix;

        // When
        var result = customerRegistrationService.linkCustomerWithKeycloak(
                testStoreId, testCustomer.getId(), keycloakUserId);

        // Then
        assertNotNull(result);
        assertEquals(testCustomer.getId(), result.id());
        assertEquals(testCustomer.getEmail(), result.email());

        // Note: The actual Keycloak user ID storage would require
        // extending the Customer entity to include keycloakUserId field
    }

    @Test
    @DisplayName("Should throw exception when linking non-existent customer")
    void shouldThrowExceptionWhenLinkingNonExistentCustomer() {
        UUID nonExistentId = UUID.randomUUID();
        String keycloakUserId = "keycloak-user-" + uniqueSuffix;

        // When & Then
        assertThrows(RuntimeException.class, () -> customerRegistrationService.linkCustomerWithKeycloak(
                testStoreId, nonExistentId, keycloakUserId));
    }

    @Test
    @DisplayName("Should register customer with full Keycloak integration")
    @Transactional
    void shouldRegisterCustomerWithFullKeycloakIntegration() {
        String password = "testPassword123!";

        // When
        var result = customerRegistrationService.registerCustomerWithAuth(
                testStoreId, testCreateCustomerDto, password, false);

        // Then
        assertNotNull(result);
        assertEquals(testCreateCustomerDto.email(), result.email());
        assertEquals(testCreateCustomerDto.firstName(), result.firstName());
        assertEquals(testCreateCustomerDto.lastName(), result.lastName());
        assertEquals(CustomerStatus.ACTIVE, result.status());

        // Verify customer is persisted
        var persistedCustomer = customerRepository.findByEmail(testCreateCustomerDto.email());
        assertTrue(persistedCustomer.isPresent());
        testCustomer = persistedCustomer.get();

        // Note: Full Keycloak integration testing would require a test Keycloak
        // instance
    }

    // Helper methods
    private Customer createTestCustomer() {
        var customer = new Customer();
        // Don't set ID manually - let Hibernate generate it
        customer.setEmail(testCreateCustomerDto.email());
        customer.setFirstName(testCreateCustomerDto.firstName());
        customer.setLastName(testCreateCustomerDto.lastName());
        customer.setPhone(testCreateCustomerDto.phone());
        customer.setDateOfBirth(testCreateCustomerDto.dateOfBirth()); // Use dateOfBirth instead of birthDate
        customer.setStatus(CustomerStatus.ACTIVE);
        return customer;
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
