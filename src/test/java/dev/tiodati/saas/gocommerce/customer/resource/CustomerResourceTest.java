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

import io.quarkus.test.security.TestSecurity;

import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import dev.tiodati.saas.gocommerce.customer.dto.CustomerDto;
import dev.tiodati.saas.gocommerce.customer.entity.Customer;
import dev.tiodati.saas.gocommerce.customer.entity.CustomerStatus;
import dev.tiodati.saas.gocommerce.customer.repository.CustomerRepository;
import dev.tiodati.saas.gocommerce.store.entity.Store;
import dev.tiodati.saas.gocommerce.store.entity.StoreStatus;
import dev.tiodati.saas.gocommerce.store.service.StoreService;
import dev.tiodati.saas.gocommerce.store.SchemaManager;
import dev.tiodati.saas.gocommerce.resource.dto.CreateStoreDto;
import dev.tiodati.saas.gocommerce.testinfra.TestDatabaseManager;
import dev.tiodati.saas.gocommerce.testinfra.TestTenantResolver;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Integration tests for CustomerResource REST endpoints.
 * Tests the complete customer management API workflow using real HTTP requests
 * and database interactions.
 */
@QuarkusTest
@DisplayName("CustomerResource Integration Tests")
class CustomerResourceTest {

        /** Base path for customer API endpoints. */
        private static final String API_BASE_PATH = "/api/v1/stores";

        /** Customer repository for database operations. */
        @Inject
        private CustomerRepository customerRepository;

        @Inject
        private EntityManager em;
        
        @Inject
        private TestDatabaseManager testDatabaseManager;
        
        @Inject
        private StoreService storeService;

        /** Test store ID for multi-tenant testing. */
        private UUID testStoreId;
        /** Unique suffix for test data isolation. */
        private String uniqueSuffix;
        /** Test customer DTO for creating customers. */
        private CreateCustomerDto testCreateCustomerDto;
        /** Test customer entity for cleanup. */
        private Customer testCustomer;
        /** Test store entity for cleanup. */
        private Store testStore;
        /** Test store schema name */
        private String testStoreSchema;

        /**
         * Set up test data before each test method.
         */
        @BeforeEach
        void setUp() {
                // Generate unique test data for isolation
                uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
                
                // Create test store schema for customer operations
                testStoreSchema = "test_store_" + uniqueSuffix.replace("-", "_");
                testDatabaseManager.ensureTestStoreSchema(testStoreSchema);
                
                // Create test store in the master schema (where Store entities live)
                testStore = Store.builder()
                        .name("Test Store " + uniqueSuffix)
                        .subdomain("test-store-" + uniqueSuffix)
                        .storeKey("test-store-key-" + uniqueSuffix)
                        .schemaName(testStoreSchema) // Use the proper store schema
                        .email("test@example.com")
                        .currencyCode("USD")
                        .defaultLocale("en_US")
                        .description("Test store for integration tests")
                        .domainSuffix("gocommerce.com")
                        .status(StoreStatus.ACTIVE)
                        .build();
                
                // Save using transaction
                persistStoreInTransaction(testStore);
                testStoreId = testStore.getId();

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
                // Clean up test data in store schema
                if (testCustomer != null) {
                        customerRepository.deleteById(testCustomer.getId());
                }
                // Clean up any customers created with our test email pattern
                if (testCreateCustomerDto != null) {
                        customerRepository.findByEmail(testCreateCustomerDto.email())
                                        .ifPresent(customer -> customerRepository.deleteById(customer.getId()));
                }
                        
                // Clean up test store in master schema
                if (testStore != null && testStore.getId() != null) {
                        Store managedStore = em.find(Store.class, testStore.getId());
                        if (managedStore != null) {
                                em.remove(managedStore);
                                em.flush();
                        }
                }
        }

        // Customer Registration Tests

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should register new customer successfully")
        void shouldRegisterNewCustomerSuccessfully() {
                Response response = given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
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
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
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
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(invalidCustomerDto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(400);
        }

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should return 409 for duplicate email")
        void shouldReturnConflictForDuplicateEmail() {
                // First registration
                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(testCreateCustomerDto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201);

                // Second registration with same email
                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(testCreateCustomerDto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(409);
        }

        // Customer Retrieval Tests

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should get customer by ID")
        void shouldGetCustomerById() {
                // Create customer via API to ensure proper tenant context
                Response createResponse = given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(testCreateCustomerDto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201)
                                .extract().response();

                CustomerDto customerDto = createResponse.as(CustomerDto.class);
                testCustomer = customerRepository.findByIdOptional(customerDto.id()).orElse(null);

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/{customerId}", testStoreId,
                                                customerDto.id())
                                .then()
                                .statusCode(200)
                                .body("id", equalTo(customerDto.id().toString()))
                                .body("email", equalTo(testCreateCustomerDto.email()))
                                .body("firstName", equalTo(testCreateCustomerDto.firstName()))
                                .body("lastName", equalTo(testCreateCustomerDto.lastName()));
        }

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should return 404 for non-existent customer")
        void shouldReturnNotFoundForNonExistentCustomer() {
                UUID nonExistentId = UUID.randomUUID();

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/{customerId}", testStoreId, nonExistentId)
                                .then()
                                .statusCode(404);
        }

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should find customer by email")
        void shouldFindCustomerByEmail() {
                // Create customer via API to ensure proper tenant context
                Response createResponse = given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(testCreateCustomerDto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201)
                                .extract().response();

                CustomerDto customerDto = createResponse.as(CustomerDto.class);
                testCustomer = customerRepository.findByIdOptional(customerDto.id()).orElse(null);

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .queryParam("email", testCreateCustomerDto.email())
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/by-email", testStoreId)
                                .then()
                                .statusCode(200)
                                .body("id", equalTo(customerDto.id().toString()))
                                .body("email", equalTo(testCreateCustomerDto.email()));
        }

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should return 404 for non-existent email")
        void shouldReturnNotFoundForNonExistentEmail() {
                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .queryParam("email", "nonexistent@test.com")
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/by-email", testStoreId)
                                .then()
                                .statusCode(404);
        }

        // Customer List and Search Tests

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should list customers with pagination")
        void shouldListCustomersWithPagination() {
                // Create multiple test customers via API
                var customer1Dto = new CreateCustomerDto(
                                "customer1" + uniqueSuffix + "@test.com",
                                "Customer1" + uniqueSuffix,
                                "Test1" + uniqueSuffix,
                                "+1234567890",
                                LocalDate.of(1990, 1, 1),
                                null, null, null, null, null, null, null, false, "en");
                
                var customer2Dto = new CreateCustomerDto(
                                "customer2" + uniqueSuffix + "@test.com",
                                "Customer2" + uniqueSuffix,
                                "Test2" + uniqueSuffix,
                                "+1234567891",
                                LocalDate.of(1990, 1, 1),
                                null, null, null, null, null, null, null, false, "en");

                // Create customers via API
                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(customer1Dto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201);

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(customer2Dto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201);

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .queryParam("page", 0)
                                .queryParam("size", 10)
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(200)
                                .body(".", hasSize(greaterThanOrEqualTo(2))); // API returns a list directly, not a paginated object
        }

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should search customers by name")
        void shouldSearchCustomersByName() {
                // Create multiple test customers via API
                var customer1Dto = new CreateCustomerDto(
                                "searchcustomer1" + uniqueSuffix + "@test.com",
                                "SearchableCustomer1" + uniqueSuffix,
                                "Test1" + uniqueSuffix,
                                "+1234567890",
                                LocalDate.of(1990, 1, 1),
                                null, null, null, null, null, null, null, false, "en");
                
                var customer2Dto = new CreateCustomerDto(
                                "searchcustomer2" + uniqueSuffix + "@test.com",
                                "SearchableCustomer2" + uniqueSuffix,
                                "Test2" + uniqueSuffix,
                                "+1234567891",
                                LocalDate.of(1990, 1, 1),
                                null, null, null, null, null, null, null, false, "en");

                // Create customers via API
                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(customer1Dto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201);

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(customer2Dto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201);

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .queryParam("q", "SearchableCustomer1" + uniqueSuffix)
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/search", testStoreId)
                                .then()
                                .statusCode(200)
                                .body(".", hasSize(greaterThanOrEqualTo(1)));
        }

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should filter customers by status")
        void shouldFilterCustomersByStatus() {
                // Create multiple test customers via API
                var customer1Dto = new CreateCustomerDto(
                                "status1" + uniqueSuffix + "@test.com",
                                "Status1" + uniqueSuffix,
                                "Test1" + uniqueSuffix,
                                "+1234567890",
                                LocalDate.of(1990, 1, 1),
                                null, null, null, null, null, null, null, false, "en");
                
                var customer2Dto = new CreateCustomerDto(
                                "status2" + uniqueSuffix + "@test.com",
                                "Status2" + uniqueSuffix,
                                "Test2" + uniqueSuffix,
                                "+1234567891",
                                LocalDate.of(1990, 1, 1),
                                null, null, null, null, null, null, null, false, "en");

                // Create customers via API
                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(customer1Dto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201);

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(customer2Dto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201);

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .queryParam("status", "ACTIVE")
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(200)
                                .body(".", hasSize(greaterThanOrEqualTo(1))); // API returns a list directly, not a paginated object
        }

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should get customer count")
        void shouldGetCustomerCount() {
                // Create multiple test customers via API
                var customer1Dto = new CreateCustomerDto(
                                "count1" + uniqueSuffix + "@test.com",
                                "Count1" + uniqueSuffix,
                                "Test1" + uniqueSuffix,
                                "+1234567890",
                                LocalDate.of(1990, 1, 1),
                                null, null, null, null, null, null, null, false, "en");
                
                var customer2Dto = new CreateCustomerDto(
                                "count2" + uniqueSuffix + "@test.com",
                                "Count2" + uniqueSuffix,
                                "Test2" + uniqueSuffix,
                                "+1234567891",
                                LocalDate.of(1990, 1, 1),
                                null, null, null, null, null, null, null, false, "en");

                // Create customers via API
                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(customer1Dto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201);

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(customer2Dto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201);

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .when()
                                .get(API_BASE_PATH + "/{storeId}/customers/count", testStoreId)
                                .then()
                                .statusCode(200)
                                .body("count", greaterThanOrEqualTo(2));
        }

        // Customer Update Tests

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should update customer profile")
        void shouldUpdateCustomerProfile() {
                // Create customer via API first
                Response createResponse = given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(testCreateCustomerDto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201)
                                .extract().response();

                CustomerDto originalCustomer = createResponse.as(CustomerDto.class);
                testCustomer = customerRepository.findByIdOptional(originalCustomer.id()).orElse(null);

                // Create an updated CustomerDto with the same ID
                var updateDto = new CustomerDto(
                                originalCustomer.id(), // Keep the same ID
                                originalCustomer.email(), // Keep the same email
                                "UpdatedFirst", // Update first name
                                "UpdatedLast", // Update last name
                                "+9876543210", // Update phone
                                LocalDate.of(1985, 5, 15), // Update birth date
                                originalCustomer.gender(),
                                originalCustomer.addressLine1(),
                                originalCustomer.addressLine2(),
                                originalCustomer.city(),
                                originalCustomer.stateProvince(),
                                originalCustomer.postalCode(),
                                originalCustomer.country(),
                                originalCustomer.status(),
                                originalCustomer.emailVerified(),
                                originalCustomer.marketingEmailsOptIn(),
                                originalCustomer.preferredLanguage(),
                                originalCustomer.createdAt(),
                                originalCustomer.updatedAt());

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(updateDto)
                                .when()
                                .put(API_BASE_PATH + "/{storeId}/customers/{customerId}", testStoreId,
                                                originalCustomer.id())
                                .then()
                                .statusCode(200)
                                .body("firstName", equalTo("UpdatedFirst"))
                                .body("lastName", equalTo("UpdatedLast"))
                                .body("phone", equalTo("+9876543210"));
        }

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should update customer status")
        void shouldUpdateCustomerStatus() {
                // Create customer via API first
                Response createResponse = given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(testCreateCustomerDto)
                                .when()
                                .post(API_BASE_PATH + "/{storeId}/customers", testStoreId)
                                .then()
                                .statusCode(201)
                                .extract().response();

                CustomerDto customerDto = createResponse.as(CustomerDto.class);
                testCustomer = customerRepository.findByIdOptional(customerDto.id()).orElse(null);

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .queryParam("status", "INACTIVE")
                                .when()
                                .put(API_BASE_PATH + "/{storeId}/customers/{customerId}/status", testStoreId,
                                                customerDto.id())
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("INACTIVE"));
        }

        @Test
        @TestSecurity(user = "platform-admin", roles = {"PLATFORM_ADMIN"})
        @DisplayName("Should return 404 when updating non-existent customer")
        void shouldReturnNotFoundWhenUpdatingNonExistentCustomer() {
                UUID nonExistentId = UUID.randomUUID();
                var updateDto = new CustomerDto(
                                nonExistentId, // Use the non-existent ID
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
                                CustomerStatus.ACTIVE, // status
                                true, // emailVerified
                                false, // marketingEmailsOptIn
                                "en", // preferredLanguage
                                null, // createdAt
                                null); // updatedAt

                given()
                                .header(TestTenantResolver.TENANT_HEADER, testStoreSchema)
                                .contentType(ContentType.JSON)
                                .body(updateDto)
                                .when()
                                .put(API_BASE_PATH + "/{storeId}/customers/{customerId}", testStoreId, nonExistentId)
                                .then()
                                .statusCode(404);
        }

        // Helper methods
        
        @Transactional
        public void persistStoreInTransaction(Store store) {
                store.persist();
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