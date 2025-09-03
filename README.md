# GO-Commerce

A multi-store e-commerce SaaS platform built with Quarkus 3.23.4 and event-driven architecture.

## 🚀 **MVP Status: Production Ready!**

✅ **Core Features Implemented:**
- Multi-tenant architecture with schema-per-store isolation
- Complete authentication and authorization system (Keycloak + JWT)
- Full product catalog with inventory management
- Shopping cart with persistence and calculations  
- Complete order processing workflow (cart → order → fulfillment)
- Customer management (Individual/Company types)
- Store administration and settings management
- REST APIs with OpenAPI/Swagger documentation

✅ **Production Infrastructure:**
- Docker containerization with Docker Compose orchestration
- Database migrations with Flyway (master + store schemas)
- Professional development toolchain with helper scripts
- Comprehensive testing infrastructure (unit + integration)
- Multi-tenant testing with isolated schemas

## Project Overview

GO-Commerce is a scalable, multi-store e-commerce platform designed to support multiple storefronts with complete data isolation. Each merchant operates within their own database schema, providing complete data isolation and customizable environments.

## Technology Stack

-   **Backend Framework**: Quarkus 3.23.4
-   **Database**: PostgreSQL (multi-store data), PostgreSQL (Keycloak)
-   **Authentication**: Keycloak for identity and access management
-   **Messaging**: Apache Kafka for event-driven architecture
-   **Containerization**: Docker & Docker Compose

## Development Phases

The project is being developed in phases, starting with an MVP:

### Phase 1: MVP

-   Multi-store foundation with schema-per-store approach
-   Core authentication with Keycloak integration
-   Basic product and inventory management
-   Essential customer profiles
-   Simple order processing
-   Standard pricing model

### Phase 2: Advanced Features

-   Product kits and combos
-   Advanced inventory management
-   Flexible pricing and discount rules
-   Enhanced order workflows

### Phase 3: Integration & Scale

-   Complete event-driven architecture
-   External system integrations
-   Performance optimizations
-   Advanced multi-tenancy features

## Documentation

The complete documentation for this project is available in the [Wiki](https://github.com/GO-Commerce-Platform/server/wiki):

1. [Document Structure](https://github.com/GO-Commerce-Platform/server/wiki/00-Document-Structure)
2. [Project Charter](https://github.com/GO-Commerce-Platform/server/wiki/01-Project-Charter)
3. [Roadmap](https://github.com/GO-Commerce-Platform/server/wiki/02-Roadmap)
4. [User Stories](https://github.com/GO-Commerce-Platform/server/wiki/03-User-Story)
5. [Technical Design Document](https://github.com/GO-Commerce-Platform/server/wiki/04-Technical-Design-Document)
6. [Technical Solution Specification](https://github.com/GO-Commerce-Platform/server/wiki/05-Technical-Solution-Specification)
7. [Data Model](https://github.com/GO-Commerce-Platform/server/wiki/06-Data-Model)
8. [Test Plan](https://github.com/GO-Commerce-Platform/server/wiki/07-Test-Plan)
9. [MVP Planning](https://github.com/GO-Commerce-Platform/server/wiki/08-MVP-Planning)
10. [AI Agent Instructions](https://github.com/GO-Commerce-Platform/server/wiki/09-AI-Agent-Instructions.md)

### Technical Architecture Documentation

For detailed technical implementation guides, see also:

-   [Multi-Schema Architecture](./wiki/Multi-Schema-Architecture.md) - Complete guide to the schema-per-store implementation
-   [Database Migration from MariaDB to PostgreSQL](./MIGRATION-FROM-MARIADB.md) - Details about the database migration

## 🚀 Quick Start

**Get GO-Commerce running in under 5 minutes:**

```bash
# 1. Clone the repository
git clone https://github.com/GO-Commerce-Platform/server.git
cd server

# 2. Start everything with one command
./docker/run-docker.sh
```

**That's it! 🎉** The script will:
- ✅ Build the latest application code
- ✅ Start PostgreSQL databases with health checks
- ✅ Start Keycloak with realm import
- ✅ Start the GO-Commerce application
- ✅ Show you all service URLs when ready

### 🌐 **Access Your Application:**

- **Application**: http://localhost:8080
- **API Documentation (Swagger)**: http://localhost:8080/swagger-ui  
- **OpenAPI Spec**: http://localhost:8080/openapi
- **Keycloak Admin**: http://localhost:9000 (admin/admin)

### 🧪 **Run Tests:**

```bash
./docker/run-tests.sh all    # Complete test suite with code style
./docker/run-tests.sh        # Standard tests
```

### 🔄 **Need a Fresh Start?**

```bash
./docker/rebuild-docker.sh   # Complete environment rebuild
```

## Getting Started (Detailed)

1. Clone the repository

    ```
    git clone https://github.com/GO-Commerce-Platform/server.git
    ```

2. Set up environment variables

    - Review and update the `.env` file in the `docker` directory with your desired configuration settings

3. Start the application with Docker

    ```
    cd gocommerce
    ./docker/run-docker.sh
    ```

    This script builds the application and starts all required containers:

    - PostgreSQL (main application database)
    - PostgreSQL (Keycloak database - separate instance)
    - Keycloak (authentication and identity management)
    - The GO-Commerce application

4. Alternatively, run just the infrastructure in Docker and the application in dev mode

    ```
    # Start supporting services (database, Keycloak)
    cd gocommerce
    docker-compose --env-file ./docker/.env up -d

    # Run the application in dev mode in a separate terminal
    mvn quarkus:dev
    ```

5. To completely rebuild your environment (useful after pulling updates)

    ```
    cd gocommerce
    ./docker/rebuild-docker.sh
    ```

6. To run tests with Docker dependencies
    ```
    cd gocommerce
    ./docker/run-tests.sh        # Run standard tests
    ./docker/run-tests.sh all    # Run all tests
    ./docker/run-tests.sh integration  # Run integration tests
    ```

## Multi-Schema Architecture

GO-Commerce implements a **schema-per-store** multi-tenancy approach for complete data isolation between stores:

### Key Features

-   **Dynamic Schema Creation**: New store schemas are created programmatically using `SchemaManager`
-   **Runtime Migration Targeting**: Flyway migrations target specific schemas at runtime
-   **Independent Migration History**: Each store maintains its own `flyway_schema_history` table
-   **Zero Interference**: Store migrations are completely separated from main application migrations

### Usage Example

```java
@Inject
SchemaManager schemaManager;

// Create a new store schema with migrations
String storeSchema = "store_myshop";
schemaManager.createSchema(storeSchema);

// Run additional migrations on existing schema
schemaManager.migrateSchema(storeSchema);
```

### Configuration

Store migration settings are configured via `application.properties`:

```properties
# Store migration configuration
gocommerce.flyway.store-migrations.locations=db/migration/stores
gocommerce.flyway.store-migrations.table=flyway_schema_history
gocommerce.flyway.store-migrations.baseline-version=1.0.0
gocommerce.flyway.store-migrations.validate-on-migrate=true
```

## Troubleshooting

If you encounter issues or test failures, please refer to the specific error messages. You can also [create an issue](https://github.com/GO-Commerce-Platform/server/issues) with detailed logs and steps to reproduce the problem.

## Docker Structure

The Docker configuration is organized as follows:

-   `/docker` - Contains all Docker-related files
    -   `.env` - Environment variables for all services
    -   `docker-compose.yml` - Defines all services (database, Keycloak, application)
    -   `app/` - Contains Dockerfiles for different deployment scenarios
        -   `Dockerfile.jvm` - For running the application in JVM mode
        -   `Dockerfile.native` - For running the application as a native executable
        -   `Dockerfile.legacy-jar` - For running with legacy JAR packaging
        -   `Dockerfile.native-micro` - For minimal native executable containers
    -   `keycloak-config/` - Contains Keycloak realm configuration
    -   `run-docker.sh` - Helper script to build and run the application
    -   `rebuild-docker.sh` - Script to tear down and rebuild the entire environment
    -   `run-tests.sh` - Script to run tests with required Docker dependencies

## Code Organization

The project follows a **Package by Feature** approach. This means that code is organized into modules based on the primary business feature or domain it represents. Each feature package aims to be self-contained, including its own DTOs, resources (controllers), services, and models (entities) where applicable.

Key top-level feature packages include:

-   `dev.tiodati.saas.gocommerce.platform`: For platform-level administration (e.g., creating and managing stores).
-   `dev.tiodati.saas.gocommerce.store`: For functionalities related to individual store operations.
-   `dev.tiodati.saas.gocommerce.product`: For product management within a store.
-   `dev.tiodati.saas.gocommerce.user`: For customer and general user management.
-   `dev.tiodati.saas.gocommerce.auth`: For authentication, authorization, and Keycloak integration.
-   `dev.tiodati.saas.gocommerce.i18n`: For internationalization.
-   `dev.tiodati.saas.gocommerce.shared` (or `common`): For truly cross-cutting concerns like utilities, base classes, and shared configurations not specific to a single feature.

This structure promotes high cohesion within feature modules and low coupling between them, enhancing maintainability and scalability.

## 🚀 API Overview

GO-Commerce provides a comprehensive REST API with full OpenAPI 3.1 documentation:

### 📈 **Core API Endpoints:**

**Authentication & Authorization:**
- `POST /api/auth/login` - User authentication
- `POST /api/auth/refresh` - Token refresh
- `DELETE /api/auth/logout` - User logout
- `GET /api/auth/validate` - Token validation

**Store Management:**
- `GET /api/stores` - List stores (platform admin)
- `POST /api/stores` - Create new store
- `GET /api/stores/{storeId}` - Get store details
- `PUT /api/stores/{storeId}` - Update store settings

**Product Catalog:**
- `GET /api/stores/{storeId}/products` - List products with filtering
- `POST /api/stores/{storeId}/products` - Create product
- `GET /api/stores/{storeId}/products/{productId}` - Get product details
- `PUT /api/stores/{storeId}/products/{productId}` - Update product
- `DELETE /api/stores/{storeId}/products/{productId}` - Delete product

**Customer Management:**
- `GET /api/stores/{storeId}/customers` - List customers
- `POST /api/stores/{storeId}/customers` - Create customer
- `GET /api/stores/{storeId}/customers/{customerId}` - Get customer details
- `PUT /api/stores/{storeId}/customers/{customerId}` - Update customer

**Shopping Cart:**
- `GET /api/stores/{storeId}/cart` - Get current cart
- `POST /api/stores/{storeId}/cart/items` - Add item to cart
- `PUT /api/stores/{storeId}/cart/items/{itemId}` - Update cart item
- `DELETE /api/stores/{storeId}/cart/items/{itemId}` - Remove cart item
- `POST /api/stores/{storeId}/cart/checkout` - Checkout cart

**Order Processing:**
- `GET /api/stores/{storeId}/orders` - List orders
- `POST /api/stores/{storeId}/orders` - Create order
- `GET /api/stores/{storeId}/orders/{orderId}` - Get order details
- `PUT /api/stores/{storeId}/orders/{orderId}/status` - Update order status

**Inventory Management:**
- `GET /api/stores/{storeId}/inventory` - List inventory items
- `POST /api/stores/{storeId}/inventory/adjustments` - Create inventory adjustment
- `GET /api/stores/{storeId}/inventory/{productId}` - Get product inventory

### 🔒 **Security & Multi-Tenancy:**

- **JWT Authentication**: All endpoints require valid JWT tokens
- **Role-Based Access Control**: Different roles (platform-admin, store-admin, product-manager, etc.)
- **Store Context**: Automatic store context resolution and validation
- **Schema Isolation**: Each store operates in its own database schema

### 📄 **API Documentation:**

- **Interactive Docs**: http://localhost:8080/swagger-ui
- **OpenAPI Spec**: http://localhost:8080/openapi
- **Postman Collection**: Available in `/docs/api/` directory

## 👨‍💻 Development Workflow

### **Local Development:**

```bash
# 1. Start infrastructure (recommended)
./docker/run-docker.sh

# 2. Alternative: Dev mode with hot reload
mvn quarkus:dev
# or with continuous testing
mvn quarkus:dev -Dquarkus.test.continuous-testing=enabled
```

### **Building & Testing:**

```bash
# Build application
mvn clean package

# Run tests
mvn test

# Run with Docker dependencies
./docker/run-tests.sh all

# Code style check
mvn checkstyle:check
```

### **Working with Multi-Tenancy:**

```java
// Create a new store schema
@Inject SchemaManager schemaManager;
schemaManager.createSchema("store_myshop");

// Set store context for operations
StoreContext.setCurrentStore("store_myshop");
// Database operations will now target the store schema

// Use security annotations
@RequiresStoreRole("store-admin")
public void storeAdminOperation() {
    // Automatically validates role and sets store context
}
```

### **Contributing:**

1. **Fork and Clone**: Fork the repository and clone your fork
2. **Create Branch**: `git checkout -b feature/your-feature-name`
3. **Develop**: Follow SOLID principles and write tests
4. **Test**: Ensure all tests pass with `./docker/run-tests.sh all`
5. **Commit**: Use conventional commit messages
6. **Push and PR**: Create a pull request with detailed description

## License

This project is dual-licensed:

-   **For personal, educational, and non-commercial use**: GNU Affero General Public License v3.0 (AGPL-3.0)
-   **For commercial use**: A separate commercial license is required. Please see the [COMMERCIAL_LICENSE](./COMMERCIAL_LICENSE) file for details or contact us at contato@TioDaTI.dev

For more details, please see the [LICENSE](./LICENSE) file.
