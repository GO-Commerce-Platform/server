package dev.tiodati.saas.gocommerce.auth.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional; // Added import
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines the roles within the GO-Commerce application. Each role has a name, a
 * description, and a set of implied (child) roles, establishing a role
 * hierarchy.
 */
public enum Roles {
    /**
     * Platform administrator with full access to all stores and platform
     * settings. Implies all other administrative roles.
     */
    PLATFORM_ADMIN("Platform Administrator",
            "Full access to the platform and all stores.",
            new HashSet<>(Arrays.asList("STORE_ADMIN", "PRODUCT_MANAGER",
                    "ORDER_MANAGER", "CUSTOMER_SERVICE", "CUSTOMER"))),

    /**
     * Store administrator with full access to a specific store's resources and
     * settings. Implies roles necessary for managing a store.
     */
    STORE_ADMIN("Store Administrator", "Full access to a specific store.",
            new HashSet<>(Arrays.asList("PRODUCT_MANAGER", "ORDER_MANAGER",
                    "CUSTOMER_SERVICE", "CUSTOMER"))),

    /**
     * Product manager with permissions to manage products within a store.
     */
    PRODUCT_MANAGER("Product Manager", "Manages products within a store.",
            new HashSet<>(Collections.singletonList("CUSTOMER"))),

    /**
     * Order manager with permissions to manage orders within a store.
     */
    ORDER_MANAGER("Order Manager", "Manages orders within a store.",
            new HashSet<>(Collections.singletonList("CUSTOMER"))),

    /**
     * Customer service representative with permissions to handle customer
     * support within a store.
     */
    CUSTOMER_SERVICE("Customer Service",
            "Handles customer support within a store.",
            new HashSet<>(Collections.singletonList("CUSTOMER"))),

    /**
     * Regular customer with restricted access to browse products and place
     * orders. This is the base role for most authenticated users.
     */
    CUSTOMER("Customer", "Regular user with restricted access.",
            Collections.emptySet()),

    /**
     * Anonymous user, typically for unauthenticated access to public parts of
     * the store.
     */
    ANONYMOUS("Anonymous User", "Unauthenticated user.",
            Collections.emptySet());

    /**
     * The friendly, displayable name of the role.
     */
    private final String roleName;
    /**
     * A brief description of what the role entails or permits.
     */
    private final String description;
    /**
     * A set of strings representing the names of roles that are directly
     * implied by this role. This is used internally to build the full hierarchy
     * of implied roles.
     */
    private final Set<String> impliedRoleNames; // Stores names of implied roles

    /**
     * Constructor for the Roles enum.
     *
     * @param roleName         The friendly name of the role.
     * @param description      A brief description of the role's purpose.
     * @param impliedRoleNames A set of names of roles that are implied by this
     *                         role.
     */
    Roles(String roleName, String description, Set<String> impliedRoleNames) {
        this.roleName = roleName;
        this.description = description;
        this.impliedRoleNames = Collections.unmodifiableSet(impliedRoleNames);
    }

    /**
     * Gets the friendly name of the role.
     *
     * @return The role name.
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Gets the description of the role.
     *
     * @return The role description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets a set of all roles implied by this role, including transitively
     * implied roles. For example, if PLATFORM_ADMIN implies STORE_ADMIN, and
     * STORE_ADMIN implies CUSTOMER, then PLATFORM_ADMIN will also imply
     * CUSTOMER.
     *
     * @return An unmodifiable set of {@link Roles} implied by this role.
     */
    public Set<Roles> getImpliedRoles() {
        Set<Roles> implied = new HashSet<>();
        for (String impliedName : this.impliedRoleNames) {
            Optional<Roles> roleEnumOpt = fromRoleName(impliedName); // Changed
                                                                     // to
                                                                     // handle
                                                                     // Optional
            roleEnumOpt.ifPresent(roleEnum -> { // Process if Optional is
                                                // present
                implied.add(roleEnum);
                implied.addAll(roleEnum.getImpliedRoles()); // Add transitive
                                                            // roles
            });
        }
        return Collections.unmodifiableSet(implied);
    }

    /**
     * Checks if this role implies another role, directly or transitively. A
     * role always implies itself.
     *
     * @param otherRole The role to check against. Must not be null.
     * @return {@code true} if this role implies the other role, {@code false}
     *         otherwise.
     */
    public boolean implies(Roles otherRole) {
        if (otherRole == null) {
            return false;
        }
        if (this == otherRole) {
            return true;
        }
        return getImpliedRoles().contains(otherRole);
    }

    /**
     * Converts a role name string to the corresponding {@link Roles} enum
     * constant. This method is case-insensitive.
     *
     * @param roleName The string name of the role (e.g., "STORE_ADMIN", "Store
     *                 Administrator").
     * @return An {@link Optional} containing the matching {@link Roles} enum
     *         constant, or {@link Optional#empty()} if no match is found.
     */
    public static Optional<Roles> fromRoleName(String roleName) { // Return type
                                                                  // changed to
                                                                  // Optional<Roles>
        if (roleName == null || roleName.trim().isEmpty()) {
            return Optional.empty(); // Return Optional.empty()
        }
        for (Roles role : values()) {
            if (role.name().equalsIgnoreCase(roleName.trim())
                    || role.getRoleName().equalsIgnoreCase(roleName.trim())) {
                return Optional.of(role); // Return Optional.of(role)
            }
        }
        return Optional.empty(); // Return Optional.empty()
    }

    /**
     * Returns a set of all defined role names (enum constant names).
     *
     * @return An unmodifiable set of all role names.
     */
    public static Set<String> getAllRoleNames() {
        return Arrays.stream(values()).map(Roles::name)
                .collect(Collectors.toSet());
    }
}
