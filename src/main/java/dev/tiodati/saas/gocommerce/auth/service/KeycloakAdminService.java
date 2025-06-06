package dev.tiodati.saas.gocommerce.auth.service;

import java.util.ArrayList; // For collecting RoleRepresentations
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.CreatedResponseUtil; // For extracting ID
import org.keycloak.admin.client.Keycloak; // Blocking client
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import dev.tiodati.saas.gocommerce.auth.dto.KeycloakClientCreateRequest;
import dev.tiodati.saas.gocommerce.auth.dto.KeycloakUserCreateRequest;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class KeycloakAdminService {

    /**
     * Keycloak admin client for managing Keycloak resources. This client is
     * injected by the Quarkus Keycloak Admin Client extension.
     */
    @Inject
    private Keycloak keycloakAdminClient; // Injected by
                                          // quarkus-keycloak-admin-client
                                          // (blocking)

    /**
     * The target realm where clients and users will be created. This is
     * configured via application properties.
     */
    @ConfigProperty(name = "gocommerce.keycloak.target-realm", defaultValue = "gocommerce")
    private String targetRealm;

    // Getter for targetRealm to be used in tests
    public String getTargetRealm() {
        return targetRealm;
    }

    /**
     * Creates a new Keycloak client.
     *
     * @param clientCreateRequest DTO containing client details.
     * @return The UUID of the created client.
     */
    public String createClient(
            KeycloakClientCreateRequest clientCreateRequest) {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientCreateRequest.clientId());
        clientRep.setName(clientCreateRequest.name());
        clientRep.setDescription(clientCreateRequest.description());
        clientRep.setSecret(clientCreateRequest.secret());
        clientRep.setStandardFlowEnabled(
                clientCreateRequest.standardFlowEnabled());
        clientRep.setDirectAccessGrantsEnabled(
                clientCreateRequest.directAccessGrantsEnabled());
        clientRep.setServiceAccountsEnabled(
                clientCreateRequest.serviceAccountsEnabled());
        clientRep.setPublicClient(clientCreateRequest.publicClient());
        clientRep.setRedirectUris(clientCreateRequest.redirectUris());
        clientRep.setWebOrigins(clientCreateRequest.webOrigins());
        clientRep.setFullScopeAllowed(true); // Ensure full scope is allowed
        clientRep.setEnabled(true);

        Log.infof("Attempting to create Keycloak client: %s in realm %s",
                clientRep.getClientId(), targetRealm);

        ClientsResource clientsResource = keycloakAdminClient.realm(targetRealm)
                .clients();

        try (Response response = clientsResource.create(clientRep)) {
            if (response.getStatusInfo()
                    .getFamily() == Response.Status.Family.SUCCESSFUL) {
                // String createdClientId = extractIdFromResponse(response,
                // "clients"); // Old way
                String createdClientId = CreatedResponseUtil
                        .getCreatedId(response);
                Log.infof(
                        "Successfully created Keycloak client %s with UUID %s",
                        clientRep.getClientId(), createdClientId);
                return createdClientId;
            } else {
                String errorDetails = "No details";
                if (response.hasEntity()) {
                    errorDetails = response.readEntity(String.class);
                }
                Log.errorf(
                        "Failed to create Keycloak client %s. Status: %d, Details: %s",
                        clientRep.getClientId(), response.getStatus(),
                        errorDetails);
                throw new WebApplicationException(
                        "Failed to create Keycloak client: " + errorDetails,
                        response.getStatus());
            }
        } catch (WebApplicationException wae) {
            throw wae; // Re-throw if it\'s already a WebApplicationException
        } catch (Exception e) {
            Log.errorf(e, "Error in createClient for %s",
                    clientRep.getClientId());
            throw new WebApplicationException(
                    "Error creating Keycloak client: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Creates a new user in Keycloak.
     *
     * @param userCreateRequest DTO containing user details.
     * @return The UUID of the created user.
     */
    public String createUser(KeycloakUserCreateRequest userCreateRequest) {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(userCreateRequest.username());
        userRep.setEmail(userCreateRequest.email());
        userRep.setFirstName(userCreateRequest.firstName());
        userRep.setLastName(userCreateRequest.lastName());
        userRep.setEnabled(true);
        userRep.setEmailVerified(userCreateRequest.emailVerified());

        if (userCreateRequest.password() != null
                && !userCreateRequest.password().isBlank()) {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(userCreateRequest.password());
            credential.setTemporary(false);
            userRep.setCredentials(Collections.singletonList(credential));
        }

        Log.infof("Attempting to create Keycloak user: %s in realm %s",
                userRep.getUsername(), targetRealm);

        UsersResource usersResource = keycloakAdminClient.realm(targetRealm)
                .users();

        try (Response response = usersResource.create(userRep)) {
            if (response.getStatusInfo()
                    .getFamily() == Response.Status.Family.SUCCESSFUL) {
                // String createdUserId = extractIdFromResponse(response,
                // "users"); // Old way
                String createdUserId = CreatedResponseUtil
                        .getCreatedId(response);
                Log.infof("Successfully created Keycloak user %s with UUID %s",
                        userRep.getUsername(), createdUserId);
                return createdUserId;
            } else {
                String errorDetails = "No details";
                if (response.hasEntity()) {
                    errorDetails = response.readEntity(String.class);
                }
                Log.errorf(
                        "Failed to create Keycloak user %s. Status: %d, Details: %s",
                        userRep.getUsername(), response.getStatus(),
                        errorDetails);
                throw new WebApplicationException(
                        "Failed to create Keycloak user: " + errorDetails,
                        response.getStatus());
            }
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (Exception e) {
            Log.errorf(e, "Error in createUser for %s", userRep.getUsername());
            throw new WebApplicationException(
                    "Error creating Keycloak user: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Assigns specified client roles to a user.
     *
     * @param userId     The UUID of the user.
     * @param clientUuid The UUID of the client.
     * @param roleNames  A list of role names to assign.
     */
    public void assignClientRolesToUser(String userId, String clientUuid,
            List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }
        Log.infof(
                "Attempting to assign client roles %s to user %s for client %s",
                roleNames, userId, clientUuid);

        try {
            ClientResource clientResource = keycloakAdminClient
                    .realm(targetRealm).clients().get(clientUuid);
            UserResource userResource = keycloakAdminClient.realm(targetRealm)
                    .users().get(userId);

            List<RoleRepresentation> rolesToAssign = new ArrayList<>();
            for (String roleName : roleNames) {
                try {
                    RoleRepresentation role = clientResource.roles()
                            .get(roleName).toRepresentation();
                    rolesToAssign.add(role);
                } catch (jakarta.ws.rs.NotFoundException e) {
                    Log.errorf(
                            "Client role %s not found for client UUID %s in realm %s",
                            roleName, clientUuid, targetRealm);
                    throw new WebApplicationException(
                            "Client role not found: " + roleName,
                            Response.Status.NOT_FOUND);
                }
            }

            if (!rolesToAssign.isEmpty()) {
                Log.infof(
                        "Assigning client roles %s to user %s for client %s in realm %s",
                        roleNames, userId, clientUuid, targetRealm);
                userResource.roles().clientLevel(clientUuid).add(rolesToAssign);
                Log.infof("Successfully assigned client roles to user %s",
                        userId);
            }
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (Exception e) {
            Log.errorf(e, "Failed to assign client roles to user %s", userId);
            throw new WebApplicationException(
                    "Failed to assign client roles: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Assigns specified realm roles to a user.
     *
     * @param userId    The UUID of the user.
     * @param roleNames A list of realm role names to assign.
     */
    public void assignRealmRolesToUser(String userId, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }
        Log.infof("Attempting to assign realm roles %s to user %s", roleNames,
                userId);

        try {
            RolesResource realmRolesResource = keycloakAdminClient
                    .realm(targetRealm).roles();
            UserResource userResource = keycloakAdminClient.realm(targetRealm)
                    .users().get(userId);

            List<RoleRepresentation> rolesToAssign = new ArrayList<>();
            for (String roleName : roleNames) {
                try {
                    RoleRepresentation role = realmRolesResource.get(roleName)
                            .toRepresentation();
                    rolesToAssign.add(role);
                } catch (jakarta.ws.rs.NotFoundException e) {
                    Log.errorf("Realm role %s not found in realm %s", roleName,
                            targetRealm);
                    throw new WebApplicationException(
                            "Realm role not found: " + roleName,
                            Response.Status.NOT_FOUND);
                }
            }

            if (!rolesToAssign.isEmpty()) {
                Log.infof("Assigning realm roles %s to user %s in realm %s",
                        roleNames, userId, targetRealm);
                userResource.roles().realmLevel().add(rolesToAssign);
                Log.infof("Successfully assigned realm roles to user %s",
                        userId);
            }
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (Exception e) {
            Log.errorf(e, "Failed to assign realm roles to user %s", userId);
            throw new WebApplicationException(
                    "Failed to assign realm roles: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Assigns specified groups to a user.
     *
     * @param userId     The UUID of the user.
     * @param groupNames A list of group names to assign.
     */
    public void assignGroupsToUser(String userId, List<String> groupNames) {
        if (groupNames == null || groupNames.isEmpty()) {
            return;
        }
        Log.infof("Attempting to assign groups %s to user %s in realm %s",
                groupNames, userId, targetRealm);

        UserResource userResource = keycloakAdminClient.realm(targetRealm)
                .users().get(userId);
        GroupsResource groupsResource = keycloakAdminClient.realm(targetRealm)
                .groups();

        try {
            for (String groupName : groupNames) {
                List<GroupRepresentation> matchingGroups = groupsResource
                        .groups(groupName, 0, 1);
                if (matchingGroups.isEmpty()) {
                    Log.errorf("Group \'%s\' not found in realm \'%s\'",
                            groupName, targetRealm);
                    throw new WebApplicationException(
                            "Group not found: " + groupName,
                            Response.Status.NOT_FOUND);
                }
                GroupRepresentation groupToAssign = matchingGroups.get(0);
                userResource.joinGroup(groupToAssign.getId());
                Log.infof(
                        "Successfully assigned group \'%s\' (ID: %s) to user %s",
                        groupName, groupToAssign.getId(), userId);
            }
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (Exception e) {
            Log.errorf(e, "Failed to assign groups %s to user %s", groupNames,
                    userId);
            throw new WebApplicationException(
                    "Failed to assign groups: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
