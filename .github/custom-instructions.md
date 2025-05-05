# GO-Commerce Project Context

This document provides essential context about the GO-Commerce project for AI assistants. Reference this file to understand the project's architecture, conventions, and current development status.

## Project Overview

GO-Commerce is a multi-tenant e-commerce SaaS platform built with Quarkus and event-driven architecture. The platform enables multiple businesses to operate their e-commerce storefronts with complete data isolation while sharing the same application infrastructure.

## Current Development Status

- **Phase**: Currently in MVP development (Phase 1)
- **Target Completion**: Q2 2025
- **Documentation**: Complete technical specifications and planning documents available in the README and `/wiki` directory   
   - Every major change should be documented nad mentioned on changelog.md (if non existent create it)

## Technology Stack

- **Backend Framework**: Quarkus (Java)
- **Primary Database**: MariaDB (multi-tenant data using schema-per-tenant)
- **Authentication**: Keycloak with PostgreSQL
- **Messaging**: Apache Kafka for event-driven architecture
- **Containerization**: Docker & Docker Compose
- **Build Tool**: Maven
- **Testing**: JUnit, Mockito, RestAssured

## Architecture

### Multi-Tenant Design
- **Approach**: Schema-per-tenant database architecture
- **Isolation**: Complete data isolation between tenants
- **Resolution**: Subdomain-based tenant resolution (e.g., tenant1.gocommerce.com)

### Service Architecture
- **API Layer**: RESTful endpoints with OpenAPI/Swagger documentation
- **Service Layer**: Business logic services
- **Data Access Layer**: Repository pattern with Hibernate/Panache
- **Event Layer**: Event-driven communication using Kafka (planned for post-MVP)

### Security Model
- **Authentication**: Keycloak for identity management
- **Authorization**: Role-based access control (RBAC)
- **Key Roles**: Customer, Store Admin, Platform Admin

## Key Components (MVP Focus)

1. **Multi-Tenant Foundation**
   - Tenant resolver interfaces
   - Database schema isolation
   - Tenant context management

2. **Authentication Framework**
   - Keycloak integration
   - JWT token handling
   - Basic permission structure

3. **Product Management**
   - Product CRUD operations
   - Categories and attributes
   - Basic inventory management

4. **Customer Management**
   - User registration and profiles
   - Address management

5. **Order Processing**
   - Shopping cart functionality
   - Order management
   - Basic order workflows

6. **Pricing**
   - Standard pricing model
   - Basic price adjustments

## Code Organization

- `/src/main/java/dev` - Main application code
- `/src/main/resources` - Configuration files
- `/src/main/resources/db` - Database migration files
- `/wiki` - Project documentation
- `/docker-compose.yml` - Development environment setup

## Development Conventions

### Package Structure
- `dev.tenant` - Multi-tenant core functionality
- `dev.auth` - Authentication and authorization
- `dev.product` - Product management
- `dev.customer` - Customer management
- `dev.order` - Order processing

### Naming Conventions
- **Classes**: PascalCase (e.g., `ProductService`)
- **Methods/Variables**: camelCase (e.g., `getProductById`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_PAGE_SIZE`)
- **Database Tables**: snake_case (e.g., `product_category`)

### API Conventions
- RESTful endpoints following standard HTTP methods
- Resource-based URL structure
- JSON request/response bodies
- OpenAPI documentation for all endpoints

## Development Workflow

1. Local development with Docker Compose environment
2. Quarkus dev mode for rapid iteration (`./mvnw quarkus:dev`)
3. Pull request workflow for code reviews
4. CI/CD pipeline for automated testing and deployment

## Build and Deployment Guidelines

### Environment Strategy
- **Development (dev)**: For ongoing development and feature integration
- **Staging (stg)**: Pre-production environment for testing and validation
- **Production (prd)**: Live environment for end users

### Configuration Management
- Use environment-specific properties files: `application-dev.properties`, `application-stg.properties`, `application-prd.properties`
- Store sensitive configuration in environment variables or a secure vault solution
- Never hardcode credentials or environment-specific values in the codebase
- Use `%dev`, `%test`, `%stg`, and `%prod` Quarkus profiles for environment-specific code

### Build Process
- Development builds: `./mvnw clean package -Dquarkus.profile=dev`
- Staging builds: `./mvnw clean package -Dquarkus.profile=stg`
- Production builds: `./mvnw clean package -Dquarkus.profile=prod`
- Always run the full test suite before promoting builds between environments
- Use the same Docker base image across all environments for consistency

### Deployment Rules
- **Development**:
  - Automatic deployments from the main branch
  - Deployed after each successful pull request merge
  - Quick rollback capability through CI/CD pipeline

- **Staging**:
  - Weekly scheduled deployments from release candidate branches
  - Manual approval required before deployment
  - Complete regression testing after each deployment
  - Database migrations must be backward compatible

- **Production**:
  - Deployment only from tagged releases
  - Change management process required for each deployment
  - Blue/Green deployment strategy to minimize downtime
  - Automated smoke tests immediately after deployment
  - Monitoring alerts configured before deployment

### Database Management
- Development: Automatic schema updates allowed
- Staging: Schema changes through migration scripts only
- Production: Schema changes require DBA review and approval
- All environments: Data backups before any schema change

### Monitoring and Logging
- Development: Standard logging level at DEBUG
- Staging: Standard logging level at INFO with performance metrics
- Production: Standard logging level at WARN with detailed error reporting
- All critical operations must be audited across all environments

### Rollback Procedures
- Every deployment must have a documented rollback plan
- Database changes must be reversible with rollback scripts
- Configuration changes should be versioned for quick rollbacks
- Maintain previous deployment artifacts for immediate redeployment

## Phase Roadmap

### Phase 1: MVP (Current)
- Multi-tenant foundation
- Core e-commerce functionality
- Basic security model

### Phase 2: Advanced Features
- Product kits and combos
- Advanced inventory management
- Flexible pricing and discount rules

### Phase 3: Integration & Scale
- Complete event-driven architecture
- External system integrations
- Performance optimizations
- Advanced multi-tenancy features

## Resource Links

- Project Documentation: See `/wiki` directory
- Technical Design: `/wiki/04-Technical-Design-Document.md`
- Data Model: `/wiki/06-Data-Model.md`
- MVP Planning: `/wiki/08-MVP-Planning.md`

## Code Generation Rules

### General Coding Principles
- Follow SOLID principles in all implementations
- Prefer composition over inheritance
- Write unit tests for all business logic
- Validate all user inputs at the API boundary
- Use constructor dependency injection rather than field injection
- Keep methods small and focused on a single responsibility
- Avoid static methods except for utility classes
- Prefer immutability when possible

### Multi-Tenant Implementation Rules
- All repository classes must use TenantContext to filter data
- Tenant IDs must never be exposed in external APIs
- Tenant-specific configuration must be fetched from TenantConfigService
- Never query across multiple tenants in a single transaction
- Use @TenantScoped annotation for tenant-specific services

### API Design Rules
- Follow REST best practices
- Use DTOs for all API input/output (never expose entities directly)
- Implement consistent error handling across all endpoints
- Document all endpoints with OpenAPI annotations
- Add validation annotations to all DTO fields
- Use consistent naming: GET /resources, GET /resources/{id}, POST /resources, etc.
- Implement pagination for all collection endpoints

### Database Rules
- Use Flyway versioned migrations for schema changes
- Follow entity naming conventions: PascalCase for entity classes, snake_case for tables
- Don't use eager fetching - always prefer lazy loading with specific fetch joins where needed
- Use UUID for entity IDs
- Include created_at, updated_at, and version fields on all entities
- Use soft deletion for most entities (is_deleted flag)

### Service Implementation Rules
- All business logic belongs in service classes, not in resources or repositories
- Implement service interfaces for better testability
- Handle transactions at the service level
- Log all significant business events
- Use dedicated exception types for different error scenarios

### Security Rules
- Never store sensitive data in plain text
- Use Keycloak roles for authorization checks
- Implement @RolesAllowed annotations on secured methods
- Validate all data access against current tenant context
- Don't trust client-provided identifiers without verification
- Use principal from security context, never allow impersonation

### Event-Driven Architecture Rules (Phase 3)
- Use event classes with proper serialization
- Define clear event contracts with versioning
- Implement idempotent event handlers
- Design for eventual consistency
- Include correlation IDs in all events for traceability

### Java Verbosity Reduction Guidelines

- **Use Records for DTOs**: 
  - Implement all request/response DTOs as Java records
  - Use records for any simple data-carrying classes
  - Example: `public record ProductDto(UUID id, String name, String description, BigDecimal price) {}`

- **Apply Lombok Strategically**: 
  - Use `@Builder` for complex objects that benefit from builder pattern
  - Apply `@Data` for mutable data classes (but prefer records for DTOs)
  - Use `@Value` for immutable classes that can't be records
  - Prefer `@RequiredArgsConstructor` with final fields over field injection
  - Example: `@RequiredArgsConstructor @Slf4j public class ProductService { private final ProductRepository repository; }`

- **Leverage Type Inference**:
  - Use `var` for local variables when type is obvious from initialization
  - Avoid `var` when type is not immediately apparent
  - Example: `var products = productRepository.findByCategory(categoryId);`

- **Implement Fluent Interfaces**:
  - Design builders and configuration classes with fluent interfaces
  - Return `this` from setter methods to enable method chaining
  - Example: `return new ProductBuilder().withName("Product").withPrice(BigDecimal.TEN).build();`

- **Embrace Functional Programming**:
  - Use lambdas and method references with streams for collection operations
  - Implement functional interfaces for behavior parameterization
  - Use Optional properly to avoid null checks
  - Example: `products.stream().filter(p -> p.isActive()).map(ProductMapper::toDto).toList();`

- **Avoid Boilerplate Patterns**:
  - Don't create getters/setters manually (use records or Lombok)
  - Avoid traditional Builder pattern implementation (use Lombok's @Builder)
  - Use static factory methods instead of constructors for better naming
  - Example: `Product.fromDto(productDto)` instead of `new Product(productDto)`