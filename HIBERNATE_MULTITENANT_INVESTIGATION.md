# Quarkus Hibernate Multi-Tenancy Investigation Status

## Current Issue
**Hibernate MultiTenantConnectionProvider is never called despite proper configuration**

## Project Context
- **Framework**: Quarkus 3.23.4 with Hibernate ORM
- **Database**: PostgreSQL with schema-based multi-tenancy
- **Architecture**: Multi-tenant SaaS platform using schema separation
- **Test Pattern**: Dynamic test schemas per test class

## What Works ✅
1. **UnifiedTenantResolver**: Correctly resolves tenant IDs from HTTP headers (`test_customerresourcetest_*`)
2. **Database connectivity**: Successful connections, schema creation, Flyway migrations
3. **CDI registration**: All beans properly registered and discoverable
4. **Test infrastructure**: Schemas created dynamically, cleaned up properly

## Core Problem ❌
**No MultiTenantConnectionProvider methods are ever called by Hibernate**

Despite having:
- `@ApplicationScoped @Alternative @Priority(1)` TestSchemaTenantConnectionProvider
- `quarkus.hibernate-orm.multitenant=SCHEMA` configuration
- Proper CDI bean registration (confirmed in build logs)
- Multiple configuration approaches attempted

## Evidence
- No logs from `getConnection()`, `getAnyConnection()`, or `@PostConstruct` methods
- SQL queries still target default schema: `from gocommerce.customer` instead of tenant schema
- Hibernate debug logs show beans are indexed but never invoked

## Configuration Files Status
- `application.properties`: Multi-tenancy enabled, no explicit provider configuration
- `TestSchemaTenantConnectionProvider`: Annotated with `@Alternative @Priority(1)`
- `SchemaTenantConnectionProvider`: Production provider (currently enabled)
- `UnifiedTenantResolver`: Working correctly with `@PersistenceUnitExtension`

## Key Files
- `/src/test/java/dev/tiodati/saas/gocommerce/testinfra/TestSchemaTenantConnectionProvider.java`
- `/src/main/java/dev/tiodati/saas/gocommerce/store/config/SchemaTenantConnectionProvider.java`
- `/src/main/java/dev/tiodati/saas/gocommerce/store/config/UnifiedTenantResolver.java`
- `/src/test/resources/application.properties`
- `/src/main/resources/application.properties`

## Next Steps After Research
1. Apply research findings to configuration
2. Test the solution
3. Verify schema switching works correctly
4. Run full test suite to confirm multi-tenancy

## Test Command
```bash
cd /Users/aquele_dinho/Projects/gocommerce
./mvnw test -Dtest=CustomerResourceTest#testCreateCustomer -Dquarkus.log.level=INFO
```

Expected success indicators:
- `🎉 TEST MTCP: TestSchemaTenantConnectionProvider initialized!`
- `🔥 TEST MTCP: *** getConnection() CALLED *** with tenant: test_customerresourcetest_*`
- SQL queries using tenant schema: `from test_customerresourcetest_*.customer`

---
**Investigation Date**: 2025-08-23
**Status**: Awaiting deep research on Quarkus Hibernate multi-tenancy configuration
