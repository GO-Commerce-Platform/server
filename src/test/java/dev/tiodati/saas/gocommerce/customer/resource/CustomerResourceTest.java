package dev.tiodati.saas.gocommerce.customer.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.auth.dto.LoginRequest;
import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import dev.tiodati.saas.gocommerce.customer.dto.CustomerDto;
import dev.tiodati.saas.gocommerce.customer.entity.Customer;
import dev.tiodati.saas.gocommerce.customer.entity.CustomerStatus;
import dev.tiodati.saas.gocommerce.customer.repository.CustomerRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Integration tests for CustomerResource REST endpoints.
 * Tests the complete customer management API workflow using real HTTP requests
 * and database interactions.
 */
@QuarkusTest
@Transactional
@DisplayName("CustomerResource Integration Tests")
class CustomerResourceTest {

        /** Base path for customer API endpoints. */
        private static final String API_BASE_PATH = "/api/v1/stores";

        /** Customer repository for database operations. */
        @Inject
        private CustomerRepository customerRepository;

        /** Test store ID for multi-tenant testing. */
        private UUID testStoreId;
        /** Unique suffix for test data isolation. */
        private String uniqueSuffix;
        /** Test customer DTO for creating customers. */
        private CreateCustomerDto testCreateCustomerDto;
        /** Test customer entity for cleanup. */
        private Customer testCustomer;

        /**
         * Set up test data before each test method.
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
                                null, // gender
                                null, // addressLine1
                                null, // addressLine2
                                null, // city
                                null, // stateProvince
                                null, // postalCode
                                null, // country
                                false, // marketingEmailsOptIn
                                "en"); // preferredLanguage
        }

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
        @DisplayName("Should register new customer successfully")
        void shouldRegisterNewCustomerSuccessfully() {
                Response response = given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + getAuthToken())
                                .body(testCreateCustomerDto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201)
                                .body("id", notNullValue())
                                .body("email", equalTo(testCreateCustomerDto.email()))
                                .body("firstName", equalTo(testCreateCustomerDto.firstName()))
                                .body("lastName", equalTo(testCreateCustomerDto.lastName()))
                                .body("phone", equalTo(testCreateCustomerDto.phone()))
                                .body("status", equalTo("ACTIVE"))
                                .body("createdAt", notNullValue())
                                .body("updatedAt", notNullValue())
                                .extract().response();

                CustomerDto customerDto = response.as(CustomerDto.class);
                testCustomer = customerRepository.findByIdOptional(customerDto.id()).orElse(null);
                assertNotNull(testCustomer);
        }

        @Test
        @DisplayName("Should return 400 for invalid customer data")
        void shouldReturnBadRequestForInvalidCustomerData() {
                var invalidCustomerDto = new CreateCustomerDto(
                                "invalid-email", // Invalid email format
                                "", // Empty first name
                                null, // Null last name
                                "invalid-phone",
                                null, // Null birth date
                                null, // gender
                                null, // addressLine1
                                null, // addressLine2
                                null, // city
                                null, // stateProvince
                                null, // postalCode
                                null, // country
                                false, // marketingEmailsOptIn
                                "en"); // preferredLanguage

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + getAuthToken())
                                .body(invalidCustomerDto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(400);
        }

        @Test
        @DisplayName("Should return 409 for duplicate email")
        void shouldReturnConflictForDuplicateEmail() {
                // First registration
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + getAuthToken())
                                .body(testCreateCustomerDto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201);

                // Second registration with same email
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + getAuthToken())
                                .body(testCreateCustomerDto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(409);
        }

        // Customer Retrieval Tests

        @Test
        @DisplayName("Should get customer by ID")
        @Transactional
        void shouldGetCustomerById() {
                testCustomer = createTestCustomer();
                customerRepository.persist(testCustomer);

                given()
                                .header("Authorization", "Bearer " + getAuthToken())
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/{customerId}", testStoreId,
                                                testCustomer.getId())
                                .then()
                                .statusCode(200)
                                .body("id", equalTo(testCustomer.getId().toString()))
                                .body("email", equalTo(testCustomer.getEmail()))
                                .body("firstName", equalTo(testCustomer.getFirstName()))
                                .body("lastName", equalTo(testCustomer.getLastName()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent customer")
        void shouldReturnNotFoundForNonExistentCustomer() {
                UUID nonExistentId = UUID.randomUUID();

                given()
                                .header("Authorization", "Bearer " + getAuthToken())
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/{customerId}", testStoreId, nonExistentId)
                                .then()
                                .statusCode(404);
        }

        @Test
        @DisplayName("Should find customer by email")
        @Transactional
        void shouldFindCustomerByEmail() {
                testCustomer = createTestCustomer();
                customerRepository.persist(testCustomer);

                given()
                                .header("Authorization", "Bearer " + getAuthToken())
                                .queryParam("email", testCustomer.getEmail())
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/by-email", testStoreId)
                                .then()
                                .statusCode(200)
                                .body("id", equalTo(testCustomer.getId().toString()))
                                .body("email", equalTo(testCustomer.getEmail()));
        }

        @Test
        @DisplayName("Should return 404 for non-existent email")
        void shouldReturnNotFoundForNonExistentEmail() {
                given()
                                .header("Authorization", "Bearer " + getAuthToken())
                                .queryParam("email", "nonexistent@test.com")
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/by-email", testStoreId)
                                .then()
                                .statusCode(404);
        }

        // Customer List and Search Tests

        @Test
        @DisplayName("Should list customers with pagination")
        @Transactional
        void shouldListCustomersWithPagination() {
                // Create multiple test customers
                testCustomer = createTestCustomer();
                customerRepository.persist(testCustomer);

                var customer2 = createTestCustomer("customer2" + uniqueSuffix + "@test.com", "Customer2", "Test2");
                customerRepository.persist(customer2);

                given()
                                .header("Authorization", "Bearer " + getAuthToken())
                                .queryParam("page", 0)
                                .queryParam("size", 10)
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(200)
                                .body("content", hasSize(greaterThanOrEqualTo(2)))
                                .body("page", equalTo(0))
                                .body("size", equalTo(10))
                                .body("totalElements", greaterThanOrEqualTo(2));
        }

        @Test
        @DisplayName("Should search customers by name")
        @Transactional
        void shouldSearchCustomersByName() {
                // Create multiple test customers
                testCustomer = createTestCustomer();
                customerRepository.persist(testCustomer);

                var customer2 = createTestCustomer("customer2" + uniqueSuffix + "@test.com", "Customer2", "Test2");
                customerRepository.persist(customer2);

                given()
                                .header("Authorization", "Bearer " + getAuthToken())
                                .queryParam("query", testCustomer.getFirstName())
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/search", testStoreId)
                                .then()
                                .statusCode(200)
                                .body("content", hasSize(greaterThanOrEqualTo(1)))
                                .body("content[0].firstName", containsString(testCustomer.getFirstName()));
        }

        @Test
        @DisplayName("Should filter customers by status")
        @Transactional
        void shouldFilterCustomersByStatus() {
                // Create multiple test customers
                testCustomer = createTestCustomer();
                customerRepository.persist(testCustomer);

                var customer2 = createTestCustomer("customer2" + uniqueSuffix + "@test.com", "Customer2", "Test2");
                customerRepository.persist(customer2);

                given()
                                .header("Authorization", "Bearer " + getAuthToken())
                                .queryParam("status", "ACTIVE")
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(200)
                                .body("content", hasSize(greaterThanOrEqualTo(1)));
        }

        @Test
        @DisplayName("Should get customer count")
        @Transactional
        void shouldGetCustomerCount() {
                // Create multiple test customers
                testCustomer = createTestCustomer();
                customerRepository.persist(testCustomer);

                var customer2 = createTestCustomer("customer2" + uniqueSuffix + "@test.com", "Customer2", "Test2");
                customerRepository.persist(customer2);

                given()
                                .header("Authorization", "Bearer " + getAuthToken())
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/count", testStoreId)
                                .then()
                                .statusCode(200)
                                .body("count", greaterThanOrEqualTo(2));
        }

        // Customer Update Tests

        @Test
        @DisplayName("Should update customer profile")
        @Transactional
        void shouldUpdateCustomerProfile() {
                testCustomer = createTestCustomer();
                customerRepository.persist(testCustomer);

                var updateDto = new CreateCustomerDto(
                                testCustomer.getEmail(),
                                "UpdatedFirst",
                                "UpdatedLast",
                                "+9876543210",
                                LocalDate.of(1985, 5, 15),
                                null, // gender
                                null, // addressLine1
                                null, // addressLine2
                                null, // city
                                null, // stateProvince
                                null, // postalCode
                                null, // country
                                false, // marketingEmailsOptIn
                                "en"); // preferredLanguage

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + getAuthToken())
                                .body(updateDto)
                                .when()
                                .put(API_BASE_PATH + "/{storeId}/customers/{customerId}", testStoreId,
                                                testCustomer.getId())
                                .then()
                                .statusCode(200)
                                .body("firstName", equalTo("UpdatedFirst"))
                                .body("lastName", equalTo("UpdatedLast"))
                                .body("phone", equalTo("+9876543210"));
        }

        @Test
        @DisplayName("Should update customer status")
        @Transactional
        void shouldUpdateCustomerStatus() {
                testCustomer = createTestCustomer();
                customerRepository.persist(testCustomer);

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + getAuthToken())
                                .body("{\"status\": \"INACTIVE\"}")
                                .when()
                                .put(API_BASE_PATH + "/{storeId}/customers/{customerId}/status", testStoreId,
                                                testCustomer.getId())
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("INACTIVE"));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent customer")
        void shouldReturnNotFoundWhenUpdatingNonExistentCustomer() {
                UUID nonExistentId = UUID.randomUUID();
                var updateDto = new CreateCustomerDto(
                                "test@example.com",
                                "First",
                                "Last",
                                "+1234567890",
                                LocalDate.of(1990, 1, 1),
                                null, // gender
                                null, // addressLine1
                                null, // addressLine2
                                null, // city
                                null, // stateProvince
                                null, // postalCode
                                null, // country
                                false, // marketingEmailsOptIn
                                "en"); // preferredLanguage

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + getAuthToken())
                                .body(updateDto)
                                .when()
                                .put(API_BASE_PATH + "/{storeId}/customers/{customerId}", testStoreId, nonExistentId)
                                .then()
                                .statusCode(404);
        }

        // Helper methods

        /**
         * Helper method to get an authentication token for testing.
         * Uses platform-admin credentials which should have sufficient permissions.
         *
         * @return JWT access token for authenticated requests
         */
        private String getAuthToken() {
                // Try to use a user with CUSTOMER_SERVICE role instead of platform-admin
                // First try store-admin credentials that should have the right permissions
                LoginRequest loginRequest = new LoginRequest("store-admin", "store-admin");

                try {
                        return given()
                                        .contentType(ContentType.JSON)
                                        .body(loginRequest)
                                        .when()
                                        .post("/api/v1/auth/login")
                                        .then()
                                        .statusCode(200)
                                        .extract()
                                        .path("access_token");
                } catch (Exception e) {
                        // Fallback to platform-admin if store-admin doesn't work
                        LoginRequest fallbackRequest = new LoginRequest("platform-admin", "platform-admin");
                        return given()
                                        .contentType(ContentType.JSON)
                                        .body(fallbackRequest)
                                        .when()
                                        .post("/api/v1/auth/login")
                                        .then()
                                        .statusCode(200)
                                        .extract()
                                        .path("access_token");
                }
        }

        private Customer createTestCustomer() {
                return createTestCustomer(
                                testCreateCustomerDto.email(),
                                testCreateCustomerDto.firstName(),
                                testCreateCustomerDto.lastName());
        }

        private Customer createTestCustomer(String email, String firstName, String lastName) {
                var customer = new Customer();
                customer.setEmail(email);
                customer.setFirstName(firstName);
                customer.setLastName(lastName);
                customer.setPhone("+1234567890");
                customer.setDateOfBirth(LocalDate.of(1990, 1, 1));
                customer.setStatus(CustomerStatus.ACTIVE);
                return customer;
        }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
