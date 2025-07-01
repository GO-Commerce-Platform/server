# Changelog

This file will track all notable changes to the GO-Commerce project once we publish our first release.

For now, during pre-release development:

-   See GitHub Issues for feature planning: https://github.com/username/gocommerce/issues
-   See Pull Requests for implementation details: https://github.com/username/gocommerce/pulls

## [Unreleased] - Database Migration

### Changed

-   **BREAKING**: Migrated from MariaDB to PostgreSQL as the primary database
-   Updated all database configurations and dependencies to use PostgreSQL
-   Updated Docker Compose configuration to use PostgreSQL 16
-   Modified schema management to use PostgreSQL-specific syntax
-   Updated migration scripts to use `psql` instead of `mariadb` client

### Added

-   PostgreSQL-specific Flyway migration support
-   Enhanced ENUM type support with PostgreSQL
-   Better JSONB support for flexible data structures
-   Comprehensive migration documentation in `MIGRATION-FROM-MARIADB.md`

### Technical Details

-   Database driver changed from `quarkus-jdbc-mariadb` to `quarkus-jdbc-postgresql`
-   Flyway dependency updated to `flyway-database-postgresql`
-   All SQL migrations updated for PostgreSQL compatibility
-   Schema creation now uses `CREATE SCHEMA` instead of `CREATE DATABASE`
-   Enhanced multi-tenant capabilities with better schema isolation

### Documentation

-   Updated all README and wiki documentation to reflect PostgreSQL usage
-   Added detailed migration rationale and implementation guide
-   Updated Docker setup instructions
-   Corrected all code comments and test descriptions

## [0.1.0] - Upcoming First Release

-   Will include completed MVP features
