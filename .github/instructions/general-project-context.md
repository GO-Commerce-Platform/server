# GO-Commerce AI Assistant Context

This document provides essential context about the GO-Commerce project for AI assistants to better understand its structure, technologies, and conventions.

## 1. Project Overview

GO-Commerce is a multi-store e-commerce SaaS platform built with Quarkus and an event-driven architecture. It supports multiple storefronts with complete data isolation using a schema-per-store approach and Apache Kafka for asynchronous communication.

## 2. Technology Stack

-   **Backend Framework**: Quarkus
-   **Database**: MariaDB (multi-store data), PostgreSQL (Keycloak)
-   **Authentication**: Keycloak for identity and access management (OAuth2/OpenID Connect)
-   **Messaging**: Apache Kafka for event-driven architecture
-   **Containerization**: Docker & Docker Compose
-   **ORM**: Hibernate ORM with Panache
-   **Schema Migrations**: Flyway

## 3. System Architecture

-   **Multi-Store Model**: Each store operates as an isolated entity with its own database schema. Platform administrators manage all stores; store administrators manage their specific store.
-   **Store Resolution**: Stores are typically identified by subdomain (e.g., `store1.gocommerce.com`).
-   **Event-Driven Architecture**: Apache Kafka is used for asynchronous processing, service decoupling, resilience, scalability, and integration flexibility. Key events include `order.created`, `payment.completed`.
-   **API Gateway**: Handles incoming requests, rate limiting, and routing.
-   **Main Components**:
    -   Client Applications (SPA, Mobile)
    -   Admin Dashboard (SPA)
    -   GO-Commerce API Service (Quarkus App)
    -   Keycloak Auth Server
    -   Kafka Event Broker
    -   Consumer Services (Notifications, Analytics, etc.)
    -   MariaDB & PostgreSQL Databases
    -   Redis Cache

## 4. Code Organization

The project follows a **Package by Feature** approach. Code is organized into modules based on the primary business feature or domain.

**Key Top-Level Feature Packages (under `dev.tiodati.saas.gocommerce`):**

-   `platform/`: Platform-level administration (managing stores).
-   `store/`: Individual store operations.
-   `product/`: Product management within a store.
-   `user/`: Customer and general user management.
-   `auth/`: Authentication, authorization, Keycloak integration.
-   `order/`: Order processing, shopping cart.
-   `i18n/`: Internationalization.
-   `shared/` (or `common/`): Cross-cutting concerns (utilities, base classes).

**Internal Structure of Feature Packages:**
Typically includes `dto/`, `resource/` (REST controllers), `service/`, `model/` (JPA entities), `repository/`, `event/` (Kafka producers/consumers).

## 5. Data Model

-   **Multi-Store Database**: Schema-per-store design.
    -   `gocommerce_platform` (shared tables like `users`, `roles`, `store`)
    -   `gocommerce_store_{store_key}` (store-specific schemas for `products`, `orders`, `customers`, etc.)
-   **Core Entities**:
    -   `store`: Store details, status, configuration.
    -   `store_admin`: Administrative users for each store.
    -   `customer`: Base customer entity (individual, company types).
    -   `address`: Customer shipping/billing addresses.
    -   `product_category`: Hierarchical product categories.
    -   `product`: Core product entity (physical, digital, service types), uses JSON for flexible attributes.
    -   `product_inventory`: Inventory tracking.
    -   `price_list` & `product_price`: Pricing rules.
    -   `product_kit` & `product_kit_item`: Product bundles.
    -   `customer_order` & `order_item`: Order details.
    -   `event_store`: Stores domain events.
-   **JPA Entities**: PanacheEntityBase is often used. Timestamps (`createdAt`, `updatedAt`) are common.

## 6. Authentication and Authorization (RBAC)

-   **Keycloak Integration**: Roles defined in Keycloak, JWT tokens contain roles (`realm_access.roles`) and `storeId`.
-   **Role Hierarchy**: `platform-admin` > `store-admin` > `customer`.
-   **Specialized Store Roles**: `product-manager`, `order-manager`, `customer-service`.
-   **Implementation**:
    -   `@RolesAllowed`: Standard Jakarta EE for basic role checks.
    -   `@RequiresStoreRole`: Custom annotation for store-specific role checks (verifies role, store access, sets store context).
    -   `PermissionValidator`: Service for programmatic checks.
-   **Store Context**: Thread-local storage for current store schema, managed by `@RequiresStoreRole` or `StoreContext.setCurrentStore()`.
-   **Auth Endpoints**:
    -   `POST /api/auth/login`
    -   `POST /api/auth/refresh`
    -   `DELETE /api/auth/logout`
    -   `GET /api/auth/validate`

## 7. Docker Setup

-   **Configuration**: `.env` file in the `docker` directory.
-   **Services**: `mariadb`, `keycloak`, `keycloak-db` (PostgreSQL), `app` (Quarkus application).
-   **Network**: `gocommerce-network`.
-   **Key Commands**:
    -   Start: `docker compose up -d`
    -   Stop: `docker compose down`
    -   Stop & Remove Volumes: `docker compose down -v`
    -   Logs: `docker compose logs -f [service_name]`
    -   Build & Run App: `docker compose up -d --build app`
-   **Helper Scripts**:
    -   `./docker/run-docker.sh`: Build and run the application.
    -   `./docker/rebuild-docker.sh`: Tear down and rebuild.
    -   `./docker/run-tests.sh`: Run tests with Docker dependencies.

## 8. Development Workflow & Key Commands

-   **Clone**: `git clone https://github.com/aquele-dinho/GO-Commerce.git`
-   **Branching**: `username/issueXX` from `main`.
-   **Build**: `./mvnw package`
-   **Native Build**: `./mvnw package -Pnative`
-   **Run in Dev Mode (Hot Reload)**: `mvn quarkus:dev` or `./mvnw quarkus:dev`
    -   Continuous testing: `mvn quarkus:dev -Dquarkus.test.continuous-testing=enabled`
-   **Run Tests**: `mvn test` or `./mvnw test`
    -   With Docker dependencies: `./docker/run-tests.sh [all|integration]`
-   **Checkstyle**: Run `./scripts/checkstyle-report.sh` or use IDE integration. Fix basic issues with `./scripts/fix-style-basics.sh`.

## 9. Internationalization (i18n)

-   **Mechanism**: Standard Java resource bundles (`messages_xx.properties`).
-   **Supported Languages**: English (default), Brazilian Portuguese (`pt_BR`), Portuguese (`pt`), Spanish (`es`).
-   **Key Components**:
    -   `LocaleResolver` interface (default: `RequestLocaleResolver` - checks URL param, cookie, Accept-Language header).
    -   `MessageService` for retrieving translated messages.
-   **Configuration**: `application.properties` for default locale, supported locales, cookie name.
-   **REST API**: `GET /api/locale`, `POST /api/locale/{localeTag}`.

## 10. Contribution Guidelines

-   Follow SOLID principles.
-   Prefer composition over inheritance.
-   Validate inputs at API boundary.
-   Write comprehensive tests.
-   Update `CHANGELOG.md` and relevant documentation.
-   Use descriptive commit messages.
-   Create PRs with clear titles and descriptions, linking to issues.


## 11. Key File Locations

-   **README.md**: Main project entry point.
-   **Wiki (`/wiki`)**: Detailed documentation (Project Charter, Roadmap, Technical Design, Data Model, etc.).
-   **Docker Files (`/docker`)**: `docker-compose.yml`, `.env`, Dockerfiles for app.
-   **Source Code (`/src/main/java`)**: Organized by feature.
-   **Resources (`/src/main/resources`)**: `application.properties`, `import.sql`, message bundles.
-   **Tests (`/src/test/java`)**.
-   **Maven POM**: `pom.xml`.
-   **License**: `LICENSE`, `COMMERCIAL_LICENSE`.
-   **This Document**: `COPILOT_CONTEXT.md`.

This consolidated document should help in understanding the project structure and conventions for future AI-assisted development.
