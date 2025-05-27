package dev.tiodati.saas.gocommerce.auth.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import dev.tiodati.saas.gocommerce.auth.model.Roles;
import io.quarkus.logging.Log;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service to verify roles from Keycloak tokens, considering the role hierarchy.
 */
@ApplicationScoped
public class KeycloakRoleVerificationService {

    /**
     * Claim name for realm access information within the JWT.
     */
    private static final String REALM_ACCESS_CLAIM = "realm_access";

    /**
     * Claim name for resource access information within the JWT. This is
     * typically structured as "resource_access": {"client_id": {"roles":
     * ["role1", "role2"]}}.
     */
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";

    /**
     * Claim name for roles list within realm or resource access claims. This is
     * typically structured as "roles": ["role1", "role2"].
     */
    private static final String ROLES_CLAIM = "roles";

    /**
     * Claim name for the preferred username in the JWT.
     */
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";

    private final SecurityIdentity securityIdentity;
    private final JsonWebToken currentJwt;
    private final String clientId;
    private final String storeRolesClaimName;

    /**
     * Constructs a new KeycloakRoleVerificationService.
     *
     * @param securityIdentity    The current security identity.
     * @param currentJwt          The current JSON Web Token.
     * @param clientId            The OIDC client ID.
     * @param storeRolesClaimName The claim name for store-specific roles.
     */
    @Inject
    public KeycloakRoleVerificationService(SecurityIdentity securityIdentity,
            JsonWebToken currentJwt,
            @ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "gocommerce-client") String clientId,
            @ConfigProperty(name = "gocommerce.auth.store-roles-claim", defaultValue = "store_roles") String storeRolesClaimName) {
        this.securityIdentity = securityIdentity;
        this.currentJwt = currentJwt;
        this.clientId = clientId;
        this.storeRolesClaimName = storeRolesClaimName;
    }

    /**
     * Checks if the current user has the specified application role. This
     * method assumes that application roles (defined in the {@link Roles} enum)
     * are primarily managed as client roles in Keycloak.
     *
     * @param role The {@link Roles} enum value to check for. Must not be null.
     * @return {@code true} if the user has the specified role, {@code false}
     *         otherwise or if role is null.
     */
    public boolean hasRole(Roles role) {
        if (role == null) {
            Log.debug("Role to check cannot be null.");
            return false;
        }
        // This is a simplification. Depending on how roles are defined (realm
        // vs client),
        // this logic might need to be more sophisticated, potentially checking
        // both
        // or having the Roles enum provide the type of role.
        // For now, assuming application-specific roles are client roles.
        return hasClientRole(role.name());
    }

    /**
     * Checks if the current user has any of the specified application roles.
     *
     * @param roles Varargs of {@link Roles} enum values.
     * @return {@code true} if the user has at least one of the specified roles,
     *         {@code false} if no roles are provided or if the user has none of
     *         the roles.
     */
    public boolean hasAnyRole(Roles... roles) {
        if (roles == null || roles.length == 0) {
            Log.debug("No roles provided to check for hasAnyRole.");
            return false;
        }
        return Arrays.stream(roles).anyMatch(this::hasRole);
    }

    /**
     * Checks if the current user is a Platform Admin.
     *
     * @return {@code true} if the user has the {@link Roles#PLATFORM_ADMIN}
     *         role, {@code false} otherwise.
     */
    public boolean isPlatformAdmin() {
        return hasRole(Roles.PLATFORM_ADMIN);
    }

    /**
     * Checks if the current user is an Admin for the specified store. This
     * verifies if the user possesses the {@link Roles#STORE_ADMIN} role within
     * the context of the given store ID, based on store-specific claims in the
     * JWT.
     *
     * @param storeId The ID of the store. Must not be null.
     * @return {@code true} if the user is an admin for the specified store,
     *         {@code false} otherwise or if storeId is null.
     */
    public boolean isStoreAdmin(String storeId) {
        if (storeId == null) {
            Log.debug("Store ID cannot be null for isStoreAdmin check.");
            return false;
        }
        // Assumes Roles.STORE_ADMIN.name() gives the role string (e.g.,
        // "STORE_ADMIN")
        // that is expected within the store-specific roles claim.
        return hasAnyStoreRole(storeId, Roles.STORE_ADMIN.name());
    }

    /**
     * Checks if the current user has the specified realm role.
     *
     * @param requiredRole The role to check for.
     * @return True if the user has the role, false otherwise.
     */
    public boolean hasRealmRole(String requiredRole) {
        return hasRealmRole(this.currentJwt, requiredRole);
    }

    /**
     * Checks if the provided JWT has the specified realm role.
     *
     * @param jwtToken     The JWT to check.
     * @param requiredRole The role to check for.
     * @return True if the user has the role, false otherwise.
     */
    @SuppressWarnings("unchecked")
    public boolean hasRealmRole(JsonWebToken jwtToken, String requiredRole) {
        if (jwtToken == null || requiredRole == null) {
            return false;
        }
        Optional<Map<String, Object>> realmAccess = jwtToken
                .claim(REALM_ACCESS_CLAIM);
        return realmAccess
                .map(access -> ((List<String>) access.getOrDefault(ROLES_CLAIM,
                        Collections.emptyList())).contains(requiredRole))
                .orElse(false);
    }

    /**
     * Checks if the current user has the specified client role.
     *
     * @param requiredRole The client role to check for.
     * @return True if the user has the role, false otherwise.
     */
    public boolean hasClientRole(String requiredRole) {
        return hasClientRole(this.currentJwt, clientId, requiredRole);
    }

    /**
     * Checks if the provided JWT has the specified client role for the given
     * client ID.
     *
     * @param jwtToken      The JWT to check.
     * @param clientIdValue The client ID for which the role is defined.
     * @param requiredRole  The client role to check for.
     * @return True if the user has the role, false otherwise.
     */
    @SuppressWarnings("unchecked")
    public boolean hasClientRole(JsonWebToken jwtToken, String clientIdValue,
            String requiredRole) {
        if (jwtToken == null || clientIdValue == null || requiredRole == null) {
            Log.debugf(
                    "Cannot check client role. JWT: %s, ClientID: %s, Role: %s",
                    jwtToken != null, clientIdValue, requiredRole);
            return false;
        }
        Optional<Map<String, Object>> resourceAccess = jwtToken
                .claim(RESOURCE_ACCESS_CLAIM);
        return resourceAccess
                .flatMap(access -> Optional.ofNullable(
                        (Map<String, Object>) access.get(clientIdValue)))
                .map(clientAccess -> ((List<String>) clientAccess
                        .getOrDefault(ROLES_CLAIM, Collections.emptyList()))
                                .contains(requiredRole))
                .orElse(false);
    }

    /**
     * Retrieves all realm roles for the current user.
     *
     * @return A set of realm roles, or an empty set if none are found.
     */
    public Set<String> getRealmRoles() {
        return getRealmRoles(this.currentJwt);
    }

    /**
     * Retrieves all realm roles from the provided JWT.
     *
     * @param jwtToken The JWT to extract roles from.
     * @return A set of realm roles, or an empty set if none are found.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRealmRoles(JsonWebToken jwtToken) {
        if (jwtToken == null) {
            return Collections.emptySet();
        }
        Optional<Map<String, Object>> realmAccess = jwtToken
                .claim(REALM_ACCESS_CLAIM);
        return realmAccess
                .map(access -> (List<String>) access.getOrDefault(ROLES_CLAIM,
                        Collections.emptyList()))
                .map(Set::copyOf).orElse(Collections.emptySet());
    }

    /**
     * Retrieves all client roles for the current user for the configured client
     * ID.
     *
     * @return A set of client roles, or an empty set if none are found.
     */
    public Set<String> getClientRoles() {
        return getClientRoles(this.currentJwt, clientId);
    }

    /**
     * Retrieves all client roles for the provided JWT and client ID.
     *
     * @param jwtToken      The JWT to extract roles from.
     * @param clientIdValue The client ID.
     * @return A set of client roles, or an empty set if none are found.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getClientRoles(JsonWebToken jwtToken,
            String clientIdValue) {
        if (jwtToken == null || clientIdValue == null) {
            return Collections.emptySet();
        }
        Optional<Map<String, Object>> resourceAccess = jwtToken
                .claim(RESOURCE_ACCESS_CLAIM);
        return resourceAccess
                .flatMap(access -> Optional.ofNullable(
                        (Map<String, Object>) access.get(clientIdValue)))
                .map(clientAccess -> (List<String>) clientAccess
                        .getOrDefault(ROLES_CLAIM, Collections.emptyList()))
                .map(Set::copyOf).orElse(Collections.emptySet());
    }

    /**
     * Checks if the current user has any of the specified store roles for the
     * given store ID. This assumes store roles are in a custom claim like
     * "store_roles": {"store1": ["ADMIN", "EDITOR"]}.
     *
     * @param storeId       The ID of the store.
     * @param requiredRoles A list of roles, any of which would grant access.
     * @return True if the user has at least one of the required store roles for
     *         the store, false otherwise.
     */
    public boolean hasAnyStoreRole(String storeId, String... requiredRoles) {
        if (storeId == null || requiredRoles == null
                || requiredRoles.length == 0) {
            return false;
        }
        Set<String> userStoreRoles = getStoreRolesForStore(storeId);
        return Arrays.stream(requiredRoles).anyMatch(userStoreRoles::contains);
    }

    /**
     * Retrieves all store-specific roles for the current user for a given store
     * ID. Assumes store roles are in a custom claim structured like:
     * "store_roles": { "storeId1": ["ROLE_A", "ROLE_B"], "storeId2": ["ROLE_C"]
     * }.
     *
     * @param storeId The ID of the store.
     * @return A set of roles for the specified store, or an empty set if none
     *         are found.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getStoreRolesForStore(String storeId) {
        if (this.currentJwt == null || storeId == null
                || this.storeRolesClaimName == null) {
            Log.debugf(
                    "Cannot get store roles. JWT: %s, StoreID: %s, ClaimName: %s",
                    this.currentJwt != null, storeId, this.storeRolesClaimName);
            return Collections.emptySet();
        }

        Optional<Map<String, Object>> storeRolesMapClaim = this.currentJwt
                .claim(this.storeRolesClaimName);

        return storeRolesMapClaim
                .flatMap(storeRolesMap -> Optional
                        .ofNullable((List<String>) storeRolesMap.get(storeId)))
                .map(Set::copyOf).orElse(Collections.emptySet());
    }

    /**
     * Gets the username from the security identity.
     *
     * @return The username, or null if not available.
     */
    public String getUsername() {
        if (this.securityIdentity != null
                && this.securityIdentity.getPrincipal() != null) {
            return this.securityIdentity.getPrincipal().getName();
        }
        // Fallback to preferred_username from JWT if SecurityIdentity doesn't
        // provide it
        if (this.currentJwt != null
                && this.currentJwt.containsClaim(PREFERRED_USERNAME_CLAIM)) {
            return this.currentJwt.getClaim(PREFERRED_USERNAME_CLAIM);
        }
        Log.warn(
                "Username not found in SecurityIdentity or JWT preferred_username claim.");
        return null;
    }

    /**
     * Checks if the current user is anonymous.
     *
     * @return True if the user is anonymous, false otherwise.
     */
    public boolean isAnonymous() {
        return this.securityIdentity == null
                || this.securityIdentity.isAnonymous();
    }

    /**
     * Gets the User ID from the JWT's "sub" claim.
     *
     * @return The User ID (subject), or null if not available.
     */
    public String getUserId() {
        return Optional.ofNullable(this.currentJwt)
                .map(JsonWebToken::getSubject).orElse(null);
    }
}
