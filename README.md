# GO Commerce - E-Commerce API Service

This project implements a comprehensive e-commerce API service using Quarkus.

- **Project Name**: GO Commerce
- **Company Domain**: tiodati.dev
- **Package Name**: dev.tiodati.saas.gocommerce

## Features

- **Customer Management**
  - Support for both Individual and Company customers
  - Customer profiles and authentication
  - Address management

- **Product and Service Management**
  - Product catalog with categories
  - Service offerings
  - Inventory management

- **Kits/Combos Management**
  - Creating product/service bundles
  - Special pricing for combos

- **Price Policies**
  - Flexible pricing rules
  - Discount management
  - Promotional campaigns

- **Order Management**
  - Shopping cart functionality
  - Order processing workflow
  - Payment integration
  - Order tracking

- **Authentication & Authorization**
  - OAuth2 authentication with Keycloak
  - Role-based access control (RBAC)
  - JWT token validation
  - Separate customer and admin permissions

- **Multi-Tenant Architecture**
  - Schema-per-tenant model
  - Isolated data storage for each store
  - Tenant-specific customization via JSON settings
  - Subdomain-based tenant resolution
  - Support for marketplace/SaaS business model

- **Internationalization (i18n)**
  - English as the main language.
  - Brazilian Portuguese available at launch.

## Technology Stack

This project uses Quarkus, the Supersonic Subatomic Java Framework.

- **Framework**: Quarkus
- **Language**: Java
- **Database**: MariaDB (Version 11.2 used in `docker-compose.yml`)
- **Authentication**: Keycloak (OAuth2)
- **API Documentation**: OpenAPI and Swagger UI
- **ORM**: Hibernate with Panache
- **Multi-Tenancy**: Schema-based with Hibernate ORM
- **Migrations**: Flyway (Version 10.15.0+)

### Database Approach

This project uses MariaDB (currently 11.2 via Docker) as the primary database, providing:
- ACID compliance for reliable transaction processing
- JSON column support for flexible product attributes and configurations
- Strong data integrity for order and payment processing
- Simplified operations and maintenance
- Multi-schema support for tenant isolation

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Multi-Tenant Architecture

This system is designed as a multi-tenant SaaS application that supports a marketplace model where each tenant (client) has their own isolated store. Key features include:

1. **Schema Isolation**: Each tenant gets their own database schema for complete data isolation
2. **Tenant Management**: Central administration of all tenants with status tracking
3. **Custom Configuration**: JSON-based settings allow per-tenant customization
4. **Subdomain Routing**: Tenants are identified by subdomain (store.example.com)
5. **Billing Plans**: Support for different service tiers and billing models

### Setting Up a New Tenant

To create a new tenant store:

1. Use the Tenant Management API to register a new tenant:
   ```shell
   curl -X POST -H "Content-Type: application/json" \
     -d '{"name":"Example Store","subdomain":"example","billingPlan":"BASIC"}' \
     http://localhost:8080/api/admin/tenants
   ```

2. The system will automatically:
   - Create a new database schema
   - Apply all migrations to set up the tenant's tables
   - Initialize default settings

3. The tenant can then be accessed via their subdomain:
   ```
   https://example.marketplace.com/
   ```

For more details on the multi-tenant implementation, see the [PERSISTENCE.md](PERSISTENCE.md) file.

## Authentication with Keycloak

This project uses Keycloak for OAuth2-based authentication and authorization. The included `docker-compose.yml` sets up Keycloak.

1.  **Start Services:** Ensure Keycloak is running via `docker-compose up -d`. It will be available on the port defined by `KEYCLOAK_PORT` in your `.env` file (default: 9000).
2.  **Access Admin Console:** Open the Keycloak Admin Console in your browser: `http://localhost:${KEYCLOAK_PORT:-9000}/admin/`.
3.  **Login:** Log in using the admin credentials defined in your `.env` file (`KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD`, defaults are `admin`/`adminpass`).
4.  **Create Realm:**
    *   Hover over the "master" realm name in the top-left corner and click "Add realm".
    *   Enter the realm name defined in your `.env` file (`KEYCLOAK_REALM`, default: `gocommerce`).
    *   Click "Create".
5.  **Create Client:**
    *   Ensure your newly created realm (`gocommerce`) is selected in the top-left corner.
    *   Navigate to "Clients" in the left-hand menu.
    *   Click "Create client".
    *   Set the "Client ID" to the value defined in your `.env` file (`KEYCLOAK_CLIENT_ID`, default: `gocommerce-client`).
    *   Click "Next".
    *   Ensure "Client authentication" is **On**.
    *   Select only "Service accounts roles" and "Standard flow". Deselect others if necessary.
    *   Click "Next".
    *   Set "Root URL" to your application's base URL (e.g., `http://localhost:8080` if running locally).
    *   Add valid "Redirect URIs" (e.g., `http://localhost:8080/*`). You might need more specific ones depending on your application flow.
    *   Click "Save".
6.  **Get Client Secret:**
    *   After saving the client, stay on the client's page.
    *   Navigate to the "Credentials" tab.
    *   The "Client secret" is displayed here. Copy this value.
7.  **Update `.env` File:**
    *   Open your `.env` file (`/Users/aquele_dinho/Projects/gocommerce/.env`).
    *   Find or add the `KEYCLOAK_CLIENT_SECRET` variable and paste the copied secret as its value:
        ```dotenv
        KEYCLOAK_CLIENT_SECRET=PASTE_YOUR_SECRET_HERE
        ```
8.  **Configure Roles (Optional but Recommended):**
    *   Navigate to "Realm Roles" or "Client Roles" (under the "Roles" tab for your client) to define roles like `admin` and `customer`.
9.  **Create Users (Optional):**
    *   Navigate to "Users" to create test users and assign them roles under the "Role mapping" tab for each user.
10. **Verify Application Configuration:** Ensure your `application.properties` or `.env` file has the correct OIDC settings pointing to your realm and client ID, and that the application uses the `KEYCLOAK_CLIENT_SECRET` from the environment.
    ```properties
    # In application.properties (uses .env fallbacks)
    quarkus.oidc.auth-server-url=http://localhost:${KEYCLOAK_PORT:9000}/realms/${KEYCLOAK_REALM:gocommerce}
    quarkus.oidc.client-id=${KEYCLOAK_CLIENT_ID:gocommerce-client}
    quarkus.oidc.credentials.secret=${KEYCLOAK_CLIENT_SECRET:secret} # Secret comes from .env

    # Ensure these are set correctly in .env
    # KEYCLOAK_PORT=9000
    # KEYCLOAK_REALM=gocommerce
    # KEYCLOAK_CLIENT_ID=gocommerce-client
    # KEYCLOAK_CLIENT_SECRET=PASTE_YOUR_SECRET_HERE
    ```

## Database Setup

The included `docker-compose.yml` sets up MariaDB.

1.  Ensure MariaDB is running (via `docker-compose up -d`). It will be available on the port defined by `DB_PORT` in your `.env` file (default: 3306).
2.  The database name, user, and password will be taken from the `.env` file (`DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`). Defaults are defined in `docker-compose.yml` if `.env` variables are missing.
3.  Ensure your `application.properties` uses these variables (or matching defaults):
    ```properties
    quarkus.datasource.db-kind=mariadb
    quarkus.datasource.username=${DB_USERNAME:gocommerceuser}
    quarkus.datasource.password=${DB_PASSWORD:gocommercepass}
    quarkus.datasource.jdbc.url=jdbc:mariadb://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:gocommerce}
    quarkus.hibernate-orm.database.generation=update # Or validate/none depending on migration strategy
    ```

## Development Environment Setup

For development, use the included Docker Compose file (`docker-compose.yml`) and a `.env` file for configuration.

1.  Create or review the `.env` file in the project root. It should contain variables used by `docker-compose.yml` and `application.properties`. Example:
    ```dotenv
    # Database Configuration
    DB_ROOT_PASSWORD=your_strong_root_password # Mandatory for MariaDB setup
    DB_NAME=gocommerce
    DB_USERNAME=gocommerceuser
    DB_PASSWORD=gocommercepass
    DB_PORT=3306
    DB_HOST=localhost # Hostname used by the application to connect

    # Keycloak Configuration
    KEYCLOAK_PORT=9000
    KEYCLOAK_ADMIN=admin
    KEYCLOAK_ADMIN_PASSWORD=adminpass
    KEYCLOAK_REALM=gocommerce
    KEYCLOAK_CLIENT_ID=gocommerce-client
    KEYCLOAK_CLIENT_SECRET=yoursecret # Get this from Keycloak client credentials tab
    KEYCLOAK_DB_SCHEMA=keycloak # Internal Keycloak DB name
    KEYCLOAK_DB_USERNAME=keycloakuser # Internal Keycloak DB user
    KEYCLOAK_DB_PASSWORD=keycloakpass # Internal Keycloak DB password

    # Application Configuration (Optional - can also be in application.properties)
    # QUARKUS_DATASOURCE_USERNAME=${DB_USERNAME} # Example if overriding application.properties
    ```
    *Note: The `quarkus-dotenv` extension allows Quarkus to read these variables.*

2.  Start all services:
    ```shell
    docker-compose up -d
    ```
3.  This will start:
    - MariaDB database on `${DB_PORT}`
    - Keycloak server on `${KEYCLOAK_PORT}`
    - Keycloak's PostgreSQL database (internal use)
4.  Access the services:
    - Keycloak Admin Console: `http://localhost:${KEYCLOAK_PORT:-9000}/admin/`
    - Database: `jdbc:mariadb://${DB_HOST}:${DB_PORT}/${DB_NAME}`
5.  To stop all services:
    ```shell
    docker-compose down
    ```
6.  To stop services and remove volumes (**deletes all data**):
    ```shell
    docker-compose down -v
    ```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it's not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/ecommerce-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## API Documentation

API documentation is available when running the application in dev mode at <http://localhost:8080/q/swagger-ui/>.

## Development Roadmap

- [X] Set up basic project structure and dependencies
- [X] Set up persistence layer (MariaDB, Hibernate, Flyway)
- [X] Configure Docker Compose environment
- [X] Add basic multi-tenancy support (Schema-per-tenant, TenantResolver)
- [ ] Implement Customer management APIs
- [ ] Implement Product and Service management
- [ ] Develop Kits/Combos functionality
- [ ] Create Price Policies system
- [ ] Build Order management workflow
- [ ] Add authentication and authorization
- [ ] Implement payment integration
- [ ] Add comprehensive tests
- [ ] Optimize for performance

## Verbosity Reduction Directives

To keep our codebase clean and focused, we will strive to reduce Java verbosity by embracing the following principles and features:

**1. Records for Data Transfer Objects (DTOs) and Data Structures:**

* Utilize Java `record` types extensively for creating simple data-carrying classes. Records automatically provide constructors, `equals()`, `hashCode()`, and `toString()` methods, significantly reducing boilerplate for DTOs used in our MCP implementation and API interactions.
* **Example:**
    ```java
    public record ContextData(String modelId, String contextId, Map<String, Object> attributes) {}

    public record McpRequest(String action, ContextData data) {}
    ```

**2. Lombok Annotations:**

* Employ Lombok annotations to automatically generate boilerplate code for standard Java constructs:
    * `@Getter`, `@Setter`: For generating getter and setter methods for fields.
    * `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`: For constructor generation.
    * `@EqualsAndHashCode`: For generating `equals()` and `hashCode()` methods (consider using `@Data` which includes this and `@ToString`).
    * `@ToString`: For generating a `toString()` method.
    * `@Data`: A convenience annotation that combines `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode`, and `@ToString`. Use judiciously, especially for immutable data where `@Value` might be more appropriate.
    * `@Value`: For creating immutable classes with getters, `equals()`, `hashCode()`, and `toString()` (similar to records but with class syntax).

    **Note:** Ensure your IDE has the Lombok plugin installed for proper functionality.

**3. Local Variable Type Inference (`var`):**

* Use the `var` keyword for local variable declarations when the type is immediately obvious from the initializer. This can improve code readability by reducing redundant type declarations.
* **Example:**
    ```java
    var contextData = new ContextData("model-123", "context-456", Map.of("key", "value"));
    var mcpRequest = new McpRequest("process", contextData);
    ```
* **Guideline:** Use `var` to reduce clutter, but prioritize clarity. If the type is not immediately apparent, explicitly declare it.

**4. Fluent Interfaces and Method Chaining (where applicable):**

* Consider designing certain utility classes or builders with fluent interfaces to allow for more expressive and less verbose configuration or object creation.

    **Example (Conceptual):**
    ```java
   // Instead of:
   ContextData data = new ContextData("...", "...", ...);

   // We might have:
   ContextData data = ContextData.withModelAndContext("model-abc", "context-xyz");
    ```

**5. Lambdas and Functional Interfaces:**

* Leverage lambda expressions and functional interfaces (introduced in Java 8+) to express behavior concisely, especially when dealing with asynchronous operations, event handling, or stream processing within Quarkus.

    **Example:**
    ```java
    Uni.createFrom().item("data")
       .map(data -> data.toUpperCase())
       .subscribe().with(item -> System.out.println("Processed: " + item));
    ```

**6. Quarkus Features for Concise Development:**

* **Dependency Injection:** Quarkus' built-in CDI (Contexts and Dependency Injection) helps manage dependencies without excessive factory code.
* **Annotation-Driven Configuration:** Utilize Quarkus' annotations for configuration (`@ConfigProperty`), REST endpoints (`@Path`, `@GET`, `@POST`, etc.), and more, reducing the need for verbose XML or programmatic configuration.
* **Reactive Programming with Mutiny:** Embrace Quarkus' reactive programming capabilities with Mutiny for handling asynchronous operations in a more declarative and less callback-heavy style.

**7. Static Factory Methods:**

* Consider using static factory methods instead of public constructors in some cases to provide more descriptive names for object creation and potentially improve readability.

    **Example:**
    ```java
   // Instead of:
   ContextData data = new ContextData("...", "...", ...);

   // We might have:
   ContextData data = ContextData.withModelAndContext("model-abc", "context-xyz");
    ```

**By adhering to these directives, we aim to create a clean, readable, and efficient codebase for our MCP study project with Quarkus, focusing on the core logic rather than unnecessary boilerplate.** We will continuously evaluate our code and refine these guidelines as the project evolves.

## Contributing

Please read the contributing guidelines before submitting pull requests.

## Related Guides

- RESTEasy Reactive: Modern RESTful services
- Hibernate ORM with Panache: Simplify your persistence code
- Quarkus Security with OIDC: Secure your applications using OpenID Connect
- Flyway: Database migrations
- Docker Compose: Multi-container application setup
- Quarkus Dotenv: Load .env files

## Provided Code

### RESTEasy Reactive

Easily start your RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started#the-jax-rs-resources)
