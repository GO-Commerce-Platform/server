package dev.tiodati.saas.gocommerce.auth.service;

import java.util.UUID;

import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.logging.Log;

import dev.tiodati.saas.gocommerce.auth.model.Roles;
import dev.tiodati.saas.gocommerce.store.service.StoreService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PermissionValidator {
    
    private final JsonWebToken jwt;
    private final KeycloakRoleVerificationService roleVerificationService;
    
    @Inject
    public PermissionValidator(JsonWebToken jwt, 
                             StoreService storeService,
                             KeycloakRoleVerificationService roleVerificationService) {
        this.jwt = jwt;
        this.roleVerificationService = roleVerificationService;
    }
    
    /**
     * Checks if the current user has a specific role or any higher role in the hierarchy.
     */
    public boolean hasRole(Roles role) {
        if (jwt == null || jwt.getSubject() == null) {
            Log.debug("No authenticated user found when checking role: " + role);
            return false;
        }
        
        // Check if user has the specified role or any higher role
        return roleVerificationService.hasRole(jwt, role);
    }
    
    /**
     * Checks if the current user has any of the specified roles or their higher equivalents.
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
     * Checks if the current user has access to a specific store.
     */
    public boolean hasStoreAccess(UUID storeId) {
        // Platform admins have access to all stores
        if (hasRole(Roles.PLATFORM_ADMIN)) {
            return true;
        }
        
        // Extract store ID from JWT claims for store-specific users
        String userStoreId = getUserStoreId();
        if (userStoreId != null) {
            return userStoreId.equals(storeId.toString());
        }
        
        return false;
    }
    
    /**
     * Checks if the current user has a specific role for a specific store.
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
     * Gets the store ID associated with the current user from the JWT.
     */
    public String getUserStoreId() {
        if (jwt == null) {
            return null;
        }
        
        return jwt.getClaim("storeId");
    }
}