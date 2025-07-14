# Database Migration: MariaDB to PostgreSQL 

## Overview

GO-Commerce has migrated from MariaDB to PostgreSQL as the primary database system. This document explains the rationale, implementation details, and impact of this migration.

## Migration Rationale

### Why PostgreSQL?

1. **Superior Multi-Tenancy Support**

    - Better schema isolation and management
    - More robust schema-per-tenant implementation
    - Advanced SQL features for multi-tenant applications

2. **Enhanced SQL Standards Compliance**

    - Better ENUM type support
    - More consistent SQL behavior
    - Advanced JSON/JSONB support for flexible data structures

3. **Performance and Scalability**

    - Superior query optimization for complex multi-tenant queries
    - Better handling of large datasets across multiple schemas
    - More efficient schema switching and tenant isolation

4. **Development Experience**
    - More predictable behavior across different environments
    - Better tooling and ecosystem support
    - Improved debugging and monitoring capabilities

## Architecture Changes

### Database Structure

**Before (MariaDB):**

-   Single MariaDB instance for application data
-   Separate PostgreSQL instance for Keycloak

**After (PostgreSQL):**

-   PostgreSQL instance for application data (main database)
-   Separate PostgreSQL instance for Keycloak (unchanged)
-   Both instances use PostgreSQL for consistency

### Multi-Tenant Schema Management

The schema-per-store approach remains unchanged but benefits from PostgreSQL's superior schema handling:

```sql
-- Schema creation (now using PostgreSQL syntax)
CREATE SCHEMA IF NOT EXISTS gocommerce_mystore;

-- Schema-specific migrations
SET search_path TO gocommerce_mystore;
-- ... run store-specific migrations
```

## Technical Implementation

### Configuration Changes

**Database Driver:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>
```

**Connection Configuration:**

```properties
# application.properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5433/gocommerce
```

**Flyway Configuration:**

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### Docker Compose Changes

**New PostgreSQL Service:**

```yaml
postgres:
    image: postgres:16
    environment:
        POSTGRES_DB: gocommerce
        POSTGRES_USER: gocommerceuser
        POSTGRES_PASSWORD: gocommercepass
```

### Schema Manager Updates

The `SchemaManager` class now uses PostgreSQL-specific commands:

```java
// PostgreSQL schema creation
stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

// PostgreSQL schema deletion
stmt.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
```

## Migration Benefits Realized

1. **Improved Multi-Tenancy**: Better schema isolation and performance
2. **Enhanced Data Types**: Better ENUM and JSON support
3. **Standardization**: Unified PostgreSQL stack for both app and Keycloak
4. **Future-Proofing**: Better positioned for scaling and cloud deployment

## Compatibility Notes

### SQL Differences

Some SQL syntax changes were required:

**ENUM Types:**

```sql
-- PostgreSQL (new)
CREATE TYPE customer_status_type AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');

-- MariaDB (old)
-- Used VARCHAR with CHECK constraints
```

**Schema Operations:**

```sql
-- PostgreSQL (new)
CREATE SCHEMA IF NOT EXISTS store_name;
SET search_path TO store_name;

-- MariaDB (old)
CREATE DATABASE IF NOT EXISTS store_name;
USE store_name;
```

## Development Impact

### Local Development

1. **Docker Compose**: Now uses PostgreSQL for all database needs
2. **Testing**: All tests now run against PostgreSQL
3. **Migration Scripts**: Updated to use `psql` instead of `mariadb` client

### Production Considerations

-   **Database Separation**: Main app and Keycloak use separate PostgreSQL instances
-   **Backup Strategy**: Unified backup approach for both PostgreSQL instances
-   **Monitoring**: Standardized monitoring for PostgreSQL ecosystem

## Timeline

-   **Migration Completed**: June 2025
-   **Documentation Updated**: July 2025
-   **Production Deployment**: Planned for August 2025

## Future Considerations

1. **Cloud Deployment**: PostgreSQL provides better cloud-native database options
2. **Scaling**: PostgreSQL's superior replication and clustering capabilities
3. **Analytics**: Better integration with data analytics tools and services
4. **Compliance**: Enhanced security and compliance features

---

_This migration represents a significant improvement in GO-Commerce's data architecture, providing better multi-tenancy support and positioning the platform for future growth and scalability._
