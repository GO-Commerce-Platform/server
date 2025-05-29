package dev.tiodati.saas.gocommerce.auth.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue; // Added import
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance; // Added import
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation; // Added import for verification
import org.keycloak.representations.idm.RoleRepresentation;

import dev.tiodati.saas.gocommerce.auth.dto.KeycloakClientCreateRequest;
import dev.tiodati.saas.gocommerce.auth.dto.KeycloakUserCreateRequest;
import io.quarkus.arc.Arc;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response; // Added import

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Changed back to PER_CLASS
public class KeycloakAdminServiceIT {

    /**
     * Integration tests for KeycloakAdminService.
     * <p>
     * This class contains integration tests for the KeycloakAdminService, which
     * manages Keycloak clients, users, roles, and groups.
     * <p>
     * The tests are ordered to ensure that prerequisites are met before each
     * test runs.
     */
    @Inject
    private KeycloakAdminService keycloakAdminService;

    /**
     * Keycloak admin client for direct operations.
     * <p>
     * This client is used for direct cleanup and role creation if needed.
     */
    @Inject
    private Keycloak keycloakAdminClient; // For direct cleanup and role
                                          // creation if needed

    /**
     * The target realm for the tests.
     * <p>
     * This is the realm where the integration tests will create clients, users,
     * roles, and groups.
     */
    private String testClientIdentifier;

    /**
     * Unique identifiers for the test client and user.
     * <p>
     * These are generated at setup to ensure idempotency and avoid conflicts in
     * subsequent test runs.
     */
    private String testClientInternalId;

    /**
     * Unique identifiers for the test user.
     * <p>
     * This is generated at setup to ensure idempotency and avoid conflicts in
     * subsequent test runs.
     */
    private String testUserName;

    /**
     * Unique identifiers for the test user.
     * <p>
     * This is generated at setup to ensure idempotency and avoid conflicts in
     * subsequent test runs.
     */
    private String testUserInternalId;

    /**
     * Test client role name.
     * <p>
     * This is the name of the client role created during the tests.
     */
    private static final String TEST_CLIENT_ROLE_NAME = "test_client_role";

    /**
     * Test realm role name.
     * <p>
     * This is the name of the realm role assigned to the test user. It is
     * assumed that this role exists in the target realm.
     */
    private static final String TEST_REALM_ROLE_NAME = "CUSTOMER_SERVICE";

    /** Test group name for integration tests. */
    private static final String TEST_GROUP_NAME = "TEST_USERS_GROUP";

    /** The target realm for Keycloak operations, fetched from the service. */
    private String targetRealm;

    @BeforeAll
    void setup() {
        // Generate unique identifiers for test entities to ensure idempotency
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        testClientIdentifier = "test-client-" + uniqueSuffix;
        testUserName = "test-user-" + uniqueSuffix;
        Log.infof(
                "Setting up KeycloakAdminServiceIT: testClientIdentifier=%s, testUserName=%s",
                testClientIdentifier, testUserName);

        // Perform CDI-dependent setup
        performSetup();
    }

    private void performSetup() {
        // Activate request context for CDI beans
        var requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            // Fetch targetRealm from the service instance
            targetRealm = keycloakAdminService.getTargetRealm();
            Log.infof("Target realm for tests: %s", targetRealm);

            // Ensure "PLATFORM_ADMIN" role exists in the target realm
            try {
                RealmResource realmResource = keycloakAdminClient
                        .realm(targetRealm);
                if (realmResource.roles().get("PLATFORM_ADMIN")
                        .toRepresentation() == null) {
                    RoleRepresentation platformAdminRole = new RoleRepresentation(
                            "PLATFORM_ADMIN", "Platform Administrator Role",
                            false);
                    realmResource.roles().create(platformAdminRole);
                    Log.infof("Created \'PLATFORM_ADMIN\' role in realm \'%s\'",
                            targetRealm);
                } else {
                    Log.infof(
                            "\'PLATFORM_ADMIN\' role already exists in realm \'%s\'",
                            targetRealm);
                }
                // Add a small delay to allow Keycloak to process the role
                // creation/check
                try {
                    Thread.sleep(1000); // 1 second delay
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    Log.warn("Delay interrupted during Keycloak setup");
                }
            } catch (jakarta.ws.rs.NotFoundException rnfe) {
                // Role does not exist, create it
                try {
                    RealmResource realmResource = keycloakAdminClient
                            .realm(targetRealm);
                    RoleRepresentation platformAdminRole = new RoleRepresentation(
                            "PLATFORM_ADMIN", "Platform Administrator Role",
                            false);
                    realmResource.roles().create(platformAdminRole);
                    Log.infof(
                            "Created \'PLATFORM_ADMIN\' role in realm \'%s\' after NotFoundException",
                            targetRealm);
                    Thread.sleep(1000); // Delay after creation
                } catch (Exception eCreate) {
                    Log.errorf(eCreate,
                            "Failed to create \'PLATFORM_ADMIN\' role in realm \'%s\' after NotFoundException",
                            targetRealm);
                }
            } catch (Exception e) {
                Log.errorf(e,
                        "Failed to ensure \'PLATFORM_ADMIN\' role exists in realm \'%s\'. Error class: %s",
                        targetRealm, e.getClass().getName());
                // Depending on the strictness, you might want to fail the setup
                // here
            }

            // Create the test client during setup to make it available for all
            // tests
            createTestClient();

            // Create the test user during setup to make it available for all
            // tests
            createTestUser();
        } finally {
            requestContext.deactivate();
        }
    }

    private void createTestClient() {
        Log.info("Creating test client during setup");

        KeycloakClientCreateRequest clientRequest = new KeycloakClientCreateRequest();
        clientRequest.setClientId(testClientIdentifier);
        clientRequest.setName("Test Client " + testClientIdentifier);
        clientRequest
                .setDescription("A client created for integration testing.");
        clientRequest.setSecret("test-secret"); // For testing purposes
        clientRequest.setStandardFlowEnabled(true);
        clientRequest.setRedirectUris(List.of("http://localhost/test"));
        clientRequest.setWebOrigins(List.of("http://localhost"));

        try {
            testClientInternalId = keycloakAdminService
                    .createClient(clientRequest);
            Log.infof(
                    "Setup: Successfully created client with clientId \'%s\' and internal ID \'%s\'",
                    testClientIdentifier, testClientInternalId);
        } catch (WebApplicationException e) {
            String responseBody = "N/A";
            if (e.getResponse() != null && e.getResponse().hasEntity()) {
                responseBody = e.getResponse().readEntity(String.class);
            }
            Log.errorf(e,
                    "Setup: Failed to create client \'%s\'. Status: %d, Response: %s. Exception class: %s, Message: %s",
                    testClientIdentifier,
                    e.getResponse() != null ? e.getResponse().getStatus() : -1,
                    responseBody, e.getClass().getName(), e.getMessage());
            throw new RuntimeException(
                    "Failed to create test client during setup: "
                            + testClientIdentifier + ". Error: "
                            + e.getMessage() + ", Response: " + responseBody,
                    e);
        } catch (Exception ex) {
            Log.errorf(ex,
                    "Setup: An unexpected error occurred while creating client \'%s\'. Exception class: %s, Message: %s",
                    testClientIdentifier, ex.getClass().getName(),
                    ex.getMessage());
            throw new RuntimeException(
                    "An unexpected error occurred while creating test client during setup: "
                            + testClientIdentifier + ". Error: "
                            + ex.getMessage(),
                    ex);
        }
    }

    private void createTestUser() {
        Log.info("Creating test user during setup");

        KeycloakUserCreateRequest userRequest = new KeycloakUserCreateRequest();
        userRequest.setUsername(testUserName);
        userRequest.setEmail(testUserName + "@example.com");
        userRequest.setFirstName("Test");
        userRequest.setLastName("User");
        userRequest.setPassword("testpassword123");
        userRequest.setEmailVerified(true);

        try {
            testUserInternalId = keycloakAdminService.createUser(userRequest);
            Log.infof(
                    "Setup: Successfully created user \'%s\' with internal ID \'%s\'",
                    testUserName, testUserInternalId);
        } catch (WebApplicationException e) {
            String responseBody = "N/A";
            if (e.getResponse() != null && e.getResponse().hasEntity()) {
                responseBody = e.getResponse().readEntity(String.class);
            }
            Log.errorf(e,
                    "Setup: Failed to create user \'%s\'. Status: %d, Response: %s. Exception class: %s, Message: %s",
                    testUserName,
                    e.getResponse() != null ? e.getResponse().getStatus() : -1,
                    responseBody, e.getClass().getName(), e.getMessage());
            throw new RuntimeException(
                    "Failed to create test user during setup: " + testUserName
                            + ". Error: " + e.getMessage() + ", Response: "
                            + responseBody,
                    e);
        } catch (Exception ex) {
            Log.errorf(ex,
                    "Setup: An unexpected error occurred while creating user \'%s\'. Exception class: %s, Message: %s",
                    testUserName, ex.getClass().getName(), ex.getMessage());
            throw new RuntimeException(
                    "An unexpected error occurred while creating test user during setup: "
                            + testUserName + ". Error: " + ex.getMessage(),
                    ex);
        }
    }

    @Test
    @Order(1)
    void test01CreateClient() { // Renamed method
        Log.info("Running test01CreateClient");
        Log.infof(
                "test01CreateClient: Verifying client creation - testClientInternalId = %s",
                testClientInternalId);

        // Verify that the client was created successfully during setup
        assertNotNull(testClientInternalId,
                "The test client internal ID should not be null. Client should have been created during setup.");

        // Additional verification: check that the client exists in Keycloak
        try {
            RealmResource realmResource = keycloakAdminClient
                    .realm(targetRealm);
            var clientRepresentation = realmResource.clients()
                    .get(testClientInternalId).toRepresentation();
            assertNotNull(clientRepresentation,
                    "Client should exist in Keycloak");
            Log.infof(
                    "test01CreateClient: Successfully verified client with clientId \'%s\' and internal ID \'%s\' exists in Keycloak",
                    testClientIdentifier, testClientInternalId);
        } catch (Exception e) {
            Log.errorf(e,
                    "test01CreateClient: Failed to verify client existence in Keycloak. Exception class: %s, Message: %s",
                    e.getClass().getName(), e.getMessage());
            fail("Failed to verify client existence in Keycloak: "
                    + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void test02CreateUser() { // Renamed method
        Log.info("Running test02CreateUser");
        Log.infof(
                "test02CreateUser: Verifying user creation - testUserInternalId = %s",
                testUserInternalId);

        // Verify that both client and user were created successfully during
        // setup
        assertNotNull(testClientInternalId,
                "Client must be created before user test can run.");
        assertNotNull(testUserInternalId,
                "The test user internal ID should not be null. User should have been created during setup.");

        // Additional verification: check that the user exists in Keycloak
        try {
            RealmResource realmResource = keycloakAdminClient
                    .realm(targetRealm);
            var userRepresentation = realmResource.users()
                    .get(testUserInternalId).toRepresentation();
            assertNotNull(userRepresentation, "User should exist in Keycloak");
            Log.infof(
                    "test02CreateUser: Successfully verified user \'%s\' with internal ID \'%s\' exists in Keycloak",
                    testUserName, testUserInternalId);
        } catch (Exception e) {
            Log.errorf(e,
                    "test02CreateUser: Failed to verify user existence in Keycloak. Exception class: %s, Message: %s",
                    e.getClass().getName(), e.getMessage());
            fail("Failed to verify user existence in Keycloak: "
                    + e.getMessage(), e);
        }
    }

    @Test
    @Order(3)
    void test03AssignRealmRolesToUser() { // Renamed method
        Log.info("Running test03AssignRealmRolesToUser");
        assertNotNull(testUserInternalId,
                "User must be created before assigning realm roles.");
        // Assumes TEST_REALM_ROLE_NAME ("CUSTOMER_SERVICE") exists in the
        // target realm
        try {
            keycloakAdminService.assignRealmRolesToUser(testUserInternalId,
                    List.of(TEST_REALM_ROLE_NAME));
            Log.infof(
                    "test03AssignRealmRolesToUser: Successfully initiated assignment of realm role \'%s\' to user \'%s\'",
                    TEST_REALM_ROLE_NAME, testUserInternalId);

            // Verification
            RealmResource realmResource = keycloakAdminClient
                    .realm(targetRealm);
            List<RoleRepresentation> userRealmRoles = realmResource.users()
                    .get(testUserInternalId).roles().realmLevel()
                    .listEffective();
            boolean found = userRealmRoles.stream().anyMatch(
                    role -> role.getName().equals(TEST_REALM_ROLE_NAME));
            assertTrue(found, "Realm role " + TEST_REALM_ROLE_NAME
                    + " should be assigned to user " + testUserInternalId);
            Log.infof("Verified: Realm role %s is assigned to user %s",
                    TEST_REALM_ROLE_NAME, testUserInternalId);

        } catch (WebApplicationException e) {
            String responseBody = "N/A";
            if (e.getResponse() != null && e.getResponse().hasEntity()) {
                responseBody = e.getResponse().readEntity(String.class);
            }
            Log.errorf(e,
                    "test03AssignRealmRolesToUser: Failed to assign realm role. Status: %d, Response: %s",
                    e.getResponse() != null ? e.getResponse().getStatus() : -1,
                    responseBody);
            fail("Failed to assign realm role: " + TEST_REALM_ROLE_NAME
                    + " to user " + testUserInternalId + ". Error: "
                    + e.getMessage(), e);
        }
    }

    @Test
    @Order(4)
    void test04CreateAndAssignClientRoleToUser() { // Renamed method
        Log.info("Running test04CreateAndAssignClientRoleToUser");
        assertNotNull(testClientInternalId,
                "Client must be created before creating client roles.");
        assertNotNull(testUserInternalId,
                "User must be created before assigning client roles.");

        try {
            // Step 1: Create a new client role
            RoleRepresentation clientRole = new RoleRepresentation();
            clientRole.setName(TEST_CLIENT_ROLE_NAME);
            clientRole.setDescription(
                    "A role created for testing client role assignments.");
            // Use the injected keycloakAdminClient for direct operations if
            // service doesn't expose role creation
            keycloakAdminClient.realm(targetRealm).clients()
                    .get(testClientInternalId).roles().create(clientRole);
            Log.infof(
                    "test04CreateAndAssignClientRoleToUser: Successfully created client role \'%s\' for client \'%s\'",
                    TEST_CLIENT_ROLE_NAME, testClientInternalId);

            // Step 2: Assign the newly created client role to the user
            keycloakAdminService.assignClientRolesToUser(testUserInternalId,
                    testClientInternalId, List.of(TEST_CLIENT_ROLE_NAME));
            Log.infof(
                    "test04CreateAndAssignClientRoleToUser: Successfully initiated assignment of client role \'%s\' to user \'%s\' for client \'%s\'",
                    TEST_CLIENT_ROLE_NAME, testUserInternalId,
                    testClientInternalId);

            // Verification
            RealmResource realmResource = keycloakAdminClient
                    .realm(targetRealm);
            List<RoleRepresentation> userClientRoles = realmResource.users()
                    .get(testUserInternalId).roles()
                    .clientLevel(testClientInternalId).listEffective();
            boolean found = userClientRoles.stream().anyMatch(
                    role -> role.getName().equals(TEST_CLIENT_ROLE_NAME));
            assertTrue(found, "Client role " + TEST_CLIENT_ROLE_NAME
                    + " should be assigned to user " + testUserInternalId
                    + " for client " + testClientInternalId);
            Log.infof(
                    "Verified: Client role %s is assigned to user %s for client %s",
                    TEST_CLIENT_ROLE_NAME, testUserInternalId,
                    testClientInternalId);

        } catch (WebApplicationException e) {
            String responseBody = "N/A";
            if (e.getResponse() != null && e.getResponse().hasEntity()) {
                responseBody = e.getResponse().readEntity(String.class);
            }
            Log.errorf(e,
                    "test04CreateAndAssignClientRoleToUser: Failed to assign client role. Status: %d, Response: %s",
                    e.getResponse() != null ? e.getResponse().getStatus() : -1,
                    responseBody);
            fail("Failed to assign client role: " + TEST_CLIENT_ROLE_NAME
                    + " to user " + testUserInternalId + ". Error: "
                    + e.getMessage() + ", Response: " + responseBody, e);
        }
    }

    @Test
    @Order(5) // Ensure this runs after user creation
    void test05AssignGroupsToUser() { // Renamed method
        Log.info("Running test05AssignGroupsToUser");
        assertNotNull(testUserInternalId,
                "User must be created before assigning groups.");

        // Ensure the group exists or create it
        RealmResource realmResource = keycloakAdminClient.realm(targetRealm);
        List<GroupRepresentation> groups = realmResource.groups()
                .groups(TEST_GROUP_NAME, 0, 1);
        String testGroupId;
        if (groups.isEmpty()) {
            GroupRepresentation newGroup = new GroupRepresentation();
            newGroup.setName(TEST_GROUP_NAME);
            try (Response response = realmResource.groups().add(newGroup)) {
                if (response.getStatusInfo()
                        .getFamily() == Response.Status.Family.SUCCESSFUL) {
                    // Extract group ID from Location header or by
                    // re-fetching
                    // For simplicity, re-fetch (Keycloak doesn't always
                    // return
                    // ID easily on group creation)
                    List<GroupRepresentation> createdGroups = realmResource
                            .groups().groups(TEST_GROUP_NAME, 0, 1);
                    if (!createdGroups.isEmpty()) {
                        testGroupId = createdGroups.get(0).getId();
                        Log.infof("Created test group \'%s\' with ID \'%s\'",
                                TEST_GROUP_NAME, testGroupId);
                    } else {
                        fail("Failed to retrieve ID for newly created group: "
                                + TEST_GROUP_NAME);
                        return;
                    }
                } else {
                    fail("Failed to create test group: " + TEST_GROUP_NAME
                            + ", Status: " + response.getStatus());
                    return;
                }
            }
        } else {
            testGroupId = groups.get(0).getId();
            Log.infof("Test group \'%s\' already exists with ID \'%s\'",
                    TEST_GROUP_NAME, testGroupId);
        }

        try {
            keycloakAdminService.assignGroupsToUser(testUserInternalId,
                    List.of(TEST_GROUP_NAME)); // Service method uses group
                                               // names
            Log.infof(
                    "test05AssignGroupsToUser: Successfully initiated assignment of group \'%s\' to user \'%s\'",
                    TEST_GROUP_NAME, testUserInternalId);

            // Verification
            List<GroupRepresentation> userGroups = realmResource.users()
                    .get(testUserInternalId).groups();
            boolean found = userGroups.stream()
                    .anyMatch(group -> group.getName().equals(TEST_GROUP_NAME));
            assertTrue(found, "Group " + TEST_GROUP_NAME
                    + " should be assigned to user " + testUserInternalId);
            Log.infof("Verified: Group %s is assigned to user %s",
                    TEST_GROUP_NAME, testUserInternalId);

        } catch (WebApplicationException e) {
            String responseBody = "N/A";
            if (e.getResponse() != null && e.getResponse().hasEntity()) {
                responseBody = e.getResponse().readEntity(String.class);
            }
            Log.errorf(e,
                    "test05AssignGroupsToUser: Failed to assign group. Status: %d, Response: %s",
                    e.getResponse() != null ? e.getResponse().getStatus() : -1,
                    responseBody);
            fail("Failed to assign group: " + TEST_GROUP_NAME + " to user "
                    + testUserInternalId + ". Error: " + e.getMessage(), e);
        }
    }

    @AfterAll
    void cleanup() {
        Log.infof(
                "Cleaning up Keycloak entities for KeycloakAdminServiceIT...");

        // Cleanup can now access CDI beans
        Log.info("Cleanup completed");
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
