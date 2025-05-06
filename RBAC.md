# GO-Commerce Role-Based Access Control (RBAC)

This document describes the role-based access control implementation for the GO-Commerce platform.

## Role Hierarchy

The platform implements a hierarchical role system where higher roles inherit permissions from lower roles:

```
platform-admin
  └── store-admin
        └── customer
```

Additionally, there are specialized store-scoped roles:

- `product-manager`: Can manage products within a store
- `order-manager`: Can manage orders within a store
- `customer-service`: Can handle customer support within a store

### Core Roles

| Role | Description | Permissions |
|------|-------------|------------|
| `admin` | Platform administrator | Full access to all platform features and stores |
| `store-admin` | Store administrator | Full access to own store's resources |
| `user` | Regular user | Limited access to user-specific features |

### Specialized Roles

| Role | Description | Permissions |
|------|-------------|------------|
| `product-manager` | Product management | Create, update, delete products and categories |
| `order-manager` | Order management | Process and manage orders |
| `customer-service` | Customer support | View customer data and order history |

## Implementation

The RBAC system combines standard Jakarta EE security annotations with custom store-aware role checks:

### 1. Keycloak Integration

- Roles are defined in Keycloak and assigned to users
- JWT tokens contain user roles in the `realm_access.roles` claim
- Store association is stored in the JWT token as a `storeId` claim

### 2. Declarative Authorization

#### Basic Role Security

For simple role-based restrictions, use the standard `@RolesAllowed` annotation:

```java
@GET
@Path("/admin/settings")
@RolesAllowed("admin")
public Response getAdminSettings() {
    // Only users with admin role can access
}
```

#### Store-Specific Security

For store-specific role checks, combine `@RolesAllowed` with our custom `@RequiresStoreRole` annotation:

```java
@POST
@Path("/stores/{storeId}/products")
@RolesAllowed({"admin", "store-admin", "product-manager"})
@RequiresStoreRole({Role.PRODUCT_MANAGER})
public Response createProduct(@PathParam("storeId") UUID storeId, ProductDto product) {
    // Only admins, or store-admins and product managers for this specific store can access
}
```

The `@RequiresStoreRole` annotation:
- Verifies that the user has one of the specified roles
- Checks that the user has access to the specified store
- Sets the store context for the duration of the method execution

### 3. Programmatic Authorization

For programmatic role and store checks, inject the `PermissionValidator` service:

```java
@Inject
PermissionValidator permissionValidator;

public void performAction(UUID storeId) {
    // Check for a specific role
    if (permissionValidator.hasRole(Role.ADMIN)) {
        // Admin-only logic
    }
    
    // Check for store-specific role
    if (permissionValidator.hasStoreRole(storeId, Role.ORDER_MANAGER)) {
        // Store-specific order management logic
    }
}
```

## JWT Token Structure

The JWT token contains role information in the following format:

```json
{
  "exp": 1609459200,
  "iat": 1609455600,
  "jti": "2b235b1f-b10a-4155-9011-6e8e20f59989",
  "iss": "http://keycloak:8080/realms/gocommerce",
  "sub": "f:2b235b1f-b10a-4155-9011-6e8e20f59989:asmith",
  "typ": "Bearer",
  "azp": "gocommerce-client",
  "session_state": "2b235b1f-b10a-4155-9011-6e8e20f59989",
  "realm_access": {
    "roles": [
      "store-admin",
      "user"
    ]
  },
  "resource_access": {
    "gocommerce-client": {
      "roles": [
        "product-manager"
      ]
    }
  },
  "scope": "email profile",
  "email_verified": true,
  "name": "Alice Smith",
  "preferred_username": "asmith",
  "given_name": "Alice",
  "family_name": "Smith",
  "email": "alice@example.com",
  "storeId": "11111111-1111-1111-1111-111111111111"
}
```

## Store Context

The store context is a thread-local storage that holds the current store schema for multi-tenancy:

- Set automatically by `@RequiresStoreRole` for annotated methods
- Can be managed programmatically using `StoreContext.setCurrentStore()`
- Ensures data isolation between stores

## Best Practices

1. **Always use `@RequiresStoreRole` for store-scoped endpoints**
   - This ensures proper store isolation and access control

2. **Use hierarchical roles appropriately**
   - Don't explicitly check for `admin` when checking for `store-admin` as admin already includes store-admin

3. **Prefer declarative security over programmatic checks**
   - Use annotations when possible for better readability and maintenance

4. **Always validate store access for store-specific operations**
   - Ensure users can only access their own store's data

5. **Log security violations**
   - Capture and log all unauthorized access attempts for security auditing