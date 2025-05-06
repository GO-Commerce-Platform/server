package dev.tiodati.saas.gocommerce.auth.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines the roles and their hierarchy in the GO-Commerce platform.
 * This enum consolidates all application roles and establishes the role hierarchy
 * where higher roles inherit permissions from lower roles.
 * 
 * Role Hierarchy:
 * - ANONYMOUS: Base role with no inherited permissions
 * - CUSTOMER: Customer role, inherits from ANONYMOUS
 * - CUSTOMER_SERVICE: Support staff role, inherits from CUSTOMER
 * - ORDER_MANAGER: Role for managing orders, inherits from CUSTOMER_SERVICE
 * - PRODUCT_MANAGER: Role for managing products, inherits from CUSTOMER_SERVICE 
 * - STORE_ADMIN: Store administrator role, inherits from both ORDER_MANAGER and PRODUCT_MANAGER
 * - PLATFORM_ADMIN: Platform administrator role, inherits from STORE_ADMIN
 */
public enum Roles {
    // Base roles with proper role hierarchy
    ANONYMOUS("anonymous", Collections.emptySet()),
    CUSTOMER("customer", Set.of(ANONYMOUS)),
    CUSTOMER_SERVICE("customer-service", Set.of(CUSTOMER)),
    ORDER_MANAGER("order-manager", Set.of(CUSTOMER_SERVICE)),
    PRODUCT_MANAGER("product-manager", Set.of(CUSTOMER_SERVICE)),
    STORE_ADMIN("store-admin", Set.of(ORDER_MANAGER, PRODUCT_MANAGER)),
    PLATFORM_ADMIN("platform-admin", Set.of(STORE_ADMIN));

    private final String roleName;
    private final Set<Roles> directParentRoles;
    
    // This is initialized after all enum constants are created
    private Set<String> allInheritedRoleNames;

    Roles(String roleName, Set<Roles> directParentRoles) {
        this.roleName = roleName;
        this.directParentRoles = directParentRoles;
    }

    /**
     * Returns the standard string name for this role used throughout the system
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Returns direct parent roles in the hierarchy
     */
    public Set<Roles> getDirectParentRoles() {
        return directParentRoles;
    }

    /**
     * Checks if this role inherits permissions from the target role.
     *
     * @param targetRole The role whose permissions might be inherited
     * @return true if this role inherits from targetRole (or they are the same role)
     */
    public boolean inheritsFrom(Roles targetRole) {
        if (targetRole == null) {
            return false;
        }
        
        // A role always "inherits" from itself
        if (this == targetRole) {
            return true;
        }
        
        // Check direct inheritance
        if (directParentRoles.contains(targetRole)) {
            return true;
        }
        
        // Check transitive inheritance
        for (Roles parentRole : directParentRoles) {
            if (parentRole.inheritsFrom(targetRole)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Returns all roles that this role inherits from, including transitive inheritance.
     *
     * @return Set of all inherited roles, including the role itself
     */
    public Set<Roles> getAllInheritedRoles() {
        Set<Roles> result = new HashSet<>();
        
        // Always include the role itself
        result.add(this);
        
        // Add direct parent roles
        result.addAll(directParentRoles);
        
        // Add transitively inherited roles
        for (Roles parentRole : directParentRoles) {
            result.addAll(parentRole.getAllInheritedRoles());
        }
        
        return Collections.unmodifiableSet(result);
    }
    
    /**
     * Returns all role names that this role inherits from, including transitive inheritance.
     *
     * @return Set of all inherited role names, including the role itself
     */
    public Set<String> getAllInheritedRoleNames() {
        if (allInheritedRoleNames == null) {
            allInheritedRoleNames = getAllInheritedRoles().stream()
                .map(Roles::getRoleName)
                .collect(Collectors.toUnmodifiableSet());
        }
        return allInheritedRoleNames;
    }

    /**
     * Check if this role includes the specified role through the role hierarchy
     */
    public boolean includes(Roles otherRole) {
        // A role always includes itself
        if (this == otherRole) {
            return true;
        }
        
        // This role includes the other role if this role inherits from it
        return otherRole != null && this.getAllInheritedRoles().contains(otherRole);
    }

    /**
     * Get a Role enum value from a role name string
     */
    public static Roles fromName(String roleName) {
        return Arrays.stream(Roles.values())
                    .filter(role -> role.getRoleName().equals(roleName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + roleName));
    }

    /**
     * Static method to check if one role inherits from another, safely handling null values
     *
     * @param sourceRole The higher-level role being checked
     * @param targetRole The role whose permissions might be inherited
     * @return true if sourceRole inherits from targetRole (or they are the same role)
     */
    public static boolean inheritsFrom(String sourceRole, String targetRole) {
        if (sourceRole == null || targetRole == null) {
            return false;
        }
        
        try {
            Roles source = fromName(sourceRole);
            Roles target = fromName(targetRole);
            return source.inheritsFrom(target);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Static method to get all inherited role names for a role
     *
     * @param roleName The name of the role to check
     * @return Set of all inherited role names, including the role itself
     */
    public static Set<String> getAllInheritedRoleNames(String roleName) {
        try {
            Roles role = fromName(roleName);
            return role.getAllInheritedRoleNames();
        } catch (IllegalArgumentException e) {
            return Set.of(roleName);
        }
    }
}