# Changelog

All notable changes to the GO-Commerce project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- OAuth2 authentication flow with Keycloak integration
  - Created authentication endpoints for login, token refresh, and logout
  - Implemented JWT token validation and handling
  - Added support for role extraction from tokens
  - Integrated with Keycloak as the authentication provider
  - Implemented comprehensive test coverage for authentication flow

- Role-based access control (RBAC) with hierarchical roles
  - Defined role hierarchy in Keycloak realm configuration
  - Created Role enum to formalize role relationships in code
  - Implemented @RequiresStoreRole annotation for store-specific access control
  - Added StoreRoleInterceptor for enforcing store access restrictions
  - Implemented PermissionValidator service for programmatic permission checks
  - Updated resource endpoints to use new RBAC annotations
  - Comprehensive test coverage for role hierarchy and store access control
  - Added store ID validation in JWT claims
  - Documented RBAC system in RBAC.md
  - Role hierarchy: admin -> store-admin -> user
  - Support for specialized roles: product-manager, order-manager, customer-service
  - Added CUSTOMER role to support customer-specific functionality (2025-05-05)

- Internationalization (i18n) framework with support for English, Spanish, Brazilian Portuguese (pt_BR) and Portuguese
  - Implemented message bundles for all four languages
  - Created `MessageService` for accessing localized messages
  - Added locale resolution via URL, cookies, or Accept-Language header
  - Provided REST endpoints for locale management
  - Added comprehensive documentation in I18N.md
  - Added specific support for Brazilian Portuguese (pt_BR) as the secondary language

- Created Product Management API with RBAC integration
  - Implemented store-specific product endpoints
  - Applied role-based access restrictions (admin, store-admin, product-manager)
  - Added validation for product data
  - Created DTOs for data transfer with proper validation
  - Integrated with the RBAC system for store isolation

- Keycloak integration for role-based authorization
  - `KeycloakRoleVerificationService` for checking user roles
  - `@RequiresRole` annotation for basic role checking
  - `@RequiresStoreRole` annotation for store-specific role checking
  - Backward compatible `@RequiresTenantRole` annotation

- Implemented StoreBasedTenantResolver to bridge the gap between tenant and store terminology
- Added TenantResolverProducer to ensure proper tenant resolution in multi-tenant environment

### Changed
- Adopted marketplace terminology throughout the platform - referring to "stores" as "stores" to better reflect the business model
- Updated documentation and code references to align with marketplace terminology
- Added dual support for both store and store contexts to maintain backward compatibility
- Enhanced StoreContext to manage schema and store ID information
- Updated application properties for multi-tenancy configuration
- Updated entity model to use VARCHAR for UUID column types to simplify MVP development

### Fixed
- Resolved issue with locale resolution in multi-language environments
- Resolved "Could not obtain connection to query metadata" error in multi-tenant setup
- Fixed "No instance of TenantResolver was found" error by implementing proper tenant resolution
- Fixed column type mismatch for UUID fields using VARCHAR instead of BINARY

## [0.1.0] - 2023-11-15

### Added
- Initial MVP architecture setup
- Multi-store foundation with schema-per-store approach
- Core authentication with Keycloak integration
- Basic internationalization (i18n) framework
- Product catalog management (basic)
- User profile management