package dev.tiodati.saas.gocommerce.auth.service;

import java.util.UUID;

import org.eclipse.microprofile.jwt.JsonWebToken;

import dev.tiodati.saas.gocommerce.auth.model.Roles;
import dev.tiodati.saas.gocommerce.store.service.StoreService; // StoreService is injected but not used locally.
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Validates user permissions based on their JWT and roles. This service
 * combines JWT information with role verification logic to determine access
 * rights.
 */
@ApplicationScoped
public class PermissionValidator {

    /**
     * The current JSON Web Token for the authenticated user.
     */
    private final JsonWebToken jwt;
    /**
     * Service for verifying Keycloak roles.
     */
    private final KeycloakRoleVerificationService roleVerificationService;

    /**
     * Constructs a new PermissionValidator.
     *
     * @param jwt                     The current JSON Web Token.
     * @param storeService            The store service (currently unused in
     *                                this class but injected).
     * @param roleVerificationService Service for role verification.
     */
    @Inject
    public PermissionValidator(JsonWebToken jwt, StoreService storeService,
            KeycloakRoleVerificationService roleVerificationService) {
        this.jwt = jwt;
        this.roleVerificationService = roleVerificationService;
    }

    /**
     * Checks if the current user has a specific role. It first ensures a user
     * is authenticated.
     *
     * @param role The {@link Roles} to check for.
     * @return {@code true} if the user has the role, {@code false} otherwise.
     */
    public boolean hasRole(Roles role) {
        Log.debugf("Checking role: %s", role);
        
        // Handle @TestSecurity context where JWT might be null OR has null subject
        if (this.jwt == null || this.jwt.getSubject() == null) {
            Log.debug("JWT is null or has null subject, checking if we're in test security context");
            // In test context, delegate to role verification service directly
            boolean hasRole = this.roleVerificationService.hasRole(role);
            Log.debugf("Role check result (no JWT/subject) for %s: %s", role, hasRole);
            return hasRole;
        }

        Log.debugf("Checking role: %s for user: %s", role, this.jwt.getSubject());
        Log.debugf("User groups: %s", this.jwt.getGroups());
        Log.debugf("User groups: %s", this.jwt.getGroups());
        
        // Debug: Log JWT claims for troubleshooting
        Log.debugf("JWT Claims: %s", this.jwt.getClaimNames());
        Object realmAccess = this.jwt.getClaim("realm_access");
        Object resourceAccess = this.jwt.getClaim("resource_access");
        Object storeRoles = this.jwt.getClaim("store_roles");
        Log.debugf("Realm access claim: %s", realmAccess != null ? realmAccess.toString() : "null");
        Log.debugf("Resource access claim: %s", resourceAccess != null ? resourceAccess.toString() : "null");
        Log.debugf("Store roles claim: %s", storeRoles != null ? storeRoles.toString() : "null");
        
        boolean hasRole = this.roleVerificationService.hasRole(role);
        Log.debugf("Role check result for %s: %s", role, hasRole);
        return hasRole;
    }

    /**
     * Checks if the current user has any of the specified roles.
     *
     * @param roles Varargs of {@link Roles} to check for.
     * @return {@code true} if the user has at least one of the roles,
     *         {@code false} otherwise.
     */
    public boolean hasAnyRole(Roles... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }

        for (Roles role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the current user has ALL of the specified roles.
     *
     * @param roles Varargs of {@link Roles} to check for.
     * @return {@code true} if the user has all specified roles or if no roles
     *         are provided, {@code false} otherwise.
     */
    public boolean hasAllRoles(Roles... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }

        for (Roles role : roles) {
            if (!hasRole(role)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the current user has access to a specific store. Platform
     * administrators have access to all stores. Other users are checked based
     * on the 'storeId' claim in their JWT.
     *
     * @param storeId The {@link UUID} of the store to check access for.
     * @return {@code true} if the user has access, {@code false} otherwise.
     */
    public boolean hasStoreAccess(UUID storeId) {
        if (hasRole(Roles.PLATFORM_ADMIN)) {
            return true;
        }

        String userStoreId = getUserStoreId();
        if (userStoreId != null) {
            return userStoreId.equals(storeId.toString());
        }

        return false;
    }

    /**
     * Checks if the current user has a specific role for a specific store. This
     * first verifies store access and then checks for the role.
     *
     * @param storeId The {@link UUID} of the store.
     * @param role    The {@link Roles} to check for within the store context.
     * @return {@code true} if the user has the role for the store,
     *         {@code false} otherwise.
     */
    public boolean hasStoreRole(UUID storeId, Roles role) {
        // First check if the user has access to this store
        if (!hasStoreAccess(storeId)) {
            return false;
        }

        // Then check if the user has the required role
        return hasRole(role);
    }

    /**
     * Gets the store ID associated with the current user from the JWT. This
     * typically relies on a custom "storeId" claim.
     *
     * @return The store ID string if present in the JWT, or {@code null}
     *         otherwise.
     */
    public String getUserStoreId() {
        if (this.jwt == null) {
            return null;
        }

        return this.jwt.getClaim("storeId");
    }
}
