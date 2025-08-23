# Quarkus Hibernate Multi-Tenancy Configuration Issue & Solution

## Problem Analysis

After deep research into the Quarkus documentation and source code, the root cause of the `MultiTenantConnectionProvider` not being called was identified:

### Issue: Incorrect Annotation Usage with Custom Connection Provider

The custom `SchemaTenantConnectionProvider` class was trying to use `@PersistenceUnitExtension` annotation, but:

**ERROR**: `@PersistenceUnitExtension` only supports specific types:
- `org.hibernate.type.format.FormatMapper`
- `org.hibernate.resource.jdbc.spi.StatementInspector` 
- `io.quarkus.hibernate.orm.runtime.tenant.TenantResolver`
- `org.hibernate.Interceptor`
- `io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver`

**The custom `MultiTenantConnectionProvider<String>` is NOT in this supported list.**

### Issue: Wrong Approach for SCHEMA Strategy

According to Quarkus documentation research, for **SCHEMA** multi-tenancy strategy:

1. **Quarkus provides a built-in connection provider** that automatically handles schema switching
2. **Only `TenantResolver` is required** (which we already have working: `UnifiedTenantResolver`)
3. **Custom `MultiTenantConnectionProvider` is NOT needed** for SCHEMA strategy

### Issue: Mixing Quarkus Abstractions

The codebase was mixing two different approaches:
- **Hibernate native**: `MultiTenantConnectionProvider` (for raw Hibernate)
- **Quarkus abstractions**: `TenantConnectionResolver` + built-in providers (for Quarkus)

## Solution

Based on the research, the correct approach for **SCHEMA** multi-tenancy in Quarkus is:

### 1. Configuration (Already Correct) ✅
```properties
# application.properties
quarkus.hibernate-orm.multitenant=SCHEMA
```

### 2. Tenant Resolver (Already Working) ✅
```java
@ApplicationScoped
@PersistenceUnitExtension
public class UnifiedTenantResolver implements TenantResolver {
    // Already correctly implemented and working
}
```

### 3. Connection Provider (Remove Custom Implementation) ✅
- **Remove** custom `SchemaTenantConnectionProvider`
- **Let Quarkus handle schema switching automatically**
- The built-in provider will:
  1. Get tenant ID from `TenantResolver`
  2. Acquire connection from default datasource
  3. Execute `SET search_path TO "tenant_schema"` automatically

## Changes Made

1. **Disabled** `SchemaTenantConnectionProvider` completely
2. **Relies on Quarkus built-in SCHEMA strategy** 
3. **Keeps only the working components**:
   - `UnifiedTenantResolver` with `@PersistenceUnitExtension`
   - Configuration `quarkus.hibernate-orm.multitenant=SCHEMA`

## Expected Result

With this configuration, Quarkus should:
1. ✅ Call `UnifiedTenantResolver.resolveTenantId()` - **Already working**
2. ✅ Get tenant ID like `test_customerresourcetest_*` - **Already working**  
3. ✅ Use built-in connection provider to switch schema - **Should work now**
4. ✅ Execute SQL on correct tenant schema - **Should work now**

## Key Research Findings

### Quarkus Multi-Tenancy Documentation Insights:
- **SCHEMA strategy**: Only `TenantResolver` needed, Quarkus provides built-in connection provider
- **DATABASE strategy**: Both `TenantResolver` + `TenantConnectionResolver` needed
- **`@PersistenceUnitExtension`**: Required for multi-tenancy components, but only supports specific interfaces
- **Custom `MultiTenantConnectionProvider`**: Not supported directly in Quarkus (use `TenantConnectionResolver` instead)

### Alternative Approach (If Built-in Doesn't Work):
If the built-in SCHEMA provider doesn't work correctly, the alternative is:
1. Use `TenantConnectionResolver` (Quarkus abstraction) instead of `MultiTenantConnectionProvider`
2. Implement `TenantConnectionResolver.resolve(String tenantId)` returning `ConnectionProvider`
3. Apply `@PersistenceUnitExtension` annotation

## Test Command
```bash
./mvnw test -Dtest=CustomerResourceTest#testCreateCustomer -Dquarkus.log.level=INFO
```

**Expected success indicators**:
- No "relation does not exist" errors
- SQL queries use tenant schema: `from "test_customerresourcetest_*".customer`
- Tenant resolution logs show correct tenant ID

---

**Investigation Date**: 2025-08-23  
**Status**: Solution implemented based on Quarkus documentation research
