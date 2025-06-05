# Changelog

This file will track all notable changes to the GO-Commerce project once we publish our first release.

For now, during pre-release development:

-   See GitHub Issues for feature planning: https://github.com/username/gocommerce/issues
-   See Pull Requests for implementation details: https://github.com/username/gocommerce/pulls

## [Unreleased]

### Added

-   **Multi-Schema Flyway Migration Support** (#25):
    -   `SchemaManager` class for programmatic schema creation and migration
    -   Runtime schema targeting for store-specific migrations
    -   Custom configuration properties for store migrations (`gocommerce.flyway.store-migrations.*`)
    -   Independent Flyway history tables per store schema
    -   Separation of store migrations (`db/migration/stores/`) from main application migrations
-   **Enhanced Authentication Infrastructure**:
    -   Updated Keycloak integration with improved Quarkus REST client compatibility
    -   Fixed JSON field mapping in authentication responses
    -   Enhanced test infrastructure with better fallback mechanisms

### Technical Details

-   Store schemas are created dynamically using `SchemaManager.createSchema(schemaName)`
-   Flyway migrations target specific schemas at runtime via `SchemaManager.migrateSchema(schemaName)`
-   Configuration separated from main application Flyway setup to prevent interference
-   Each store maintains its own `flyway_schema_history` table for migration tracking

## [0.1.0] - Upcoming First Release

-   Will include completed MVP features
