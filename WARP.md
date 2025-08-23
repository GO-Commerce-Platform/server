# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

GO-Commerce is a multi-tenant SaaS e-commerce platform built with **Quarkus 3.23.4**. The system uses a **schema-per-store** multi-tenancy approach where each merchant operates within their own PostgreSQL schema, providing complete data isolation.

**Key Architecture:**
- **Multi-tenant**: Each store gets its own database schema (e.g., `store_myshop`)
- **Package-by-feature**: Code organized by business domain, not technical layers
- **Event-driven**: Apache Kafka for asynchronous processing and service decoupling
- **Authentication**: Keycloak with JWT and hierarchical role-based access control
- **Store Resolution**: Subdomain-based store identification (store1.gocommerce.com)

**Technology Stack:**
- Quarkus 3.23.4 (Java 21)
- PostgreSQL (app + Keycloak instances)
- Keycloak for identity management
- Hibernate ORM with Panache
- Flyway for migrations
- Apache Kafka for event streaming
- Redis for caching
- Docker & Docker Compose

**Core Business Domains:**
- Platform administration (store creation/management)
- Store operations (individual merchant context)
- Product catalog (categories, inventory, pricing)
- Customer management (individual/company types)
- Order processing (cart, checkout, fulfillment)
- Authentication/authorization (RBAC with Keycloak)

## Essential Development Commands

### Build & Run
```bash
# Development mode with hot reload
mvn quarkus:dev
# With continuous testing enabled
mvn quarkus:dev -Dquarkus.test.continuous-testing=enabled

# Build JAR
./mvnw package
# Native build (requires GraalVM)
./mvnw package -Pnative
```

### Docker Operations
**Note:** The README mentions these scripts, but they don't exist yet:
```bash
# TODO: These scripts need to be created
./docker/run-docker.sh        # Build and start all containers
./docker/rebuild-docker.sh    # Tear down and rebuild environment  
./docker/run-tests.sh         # Run tests with Docker dependencies
```

**Current Docker usage:**
```bash
# Start infrastructure only (recommended for development)
cd docker && docker-compose --env-file .env up -d

# Or start everything including the app
cd docker && docker-compose up -d
```

### Testing
```bash
# Unit and integration tests
mvn test
./mvnw test

# Specific test class
mvn test -Dtest=CustomerServiceTest
```

### Code Quality
```bash
# Run checkstyle (uses tools/checkstyle-10.3.3-all.jar)
java -jar tools/checkstyle-10.3.3-all.jar -c checkstyle.xml src/main/java/

# Apply store migrations manually (if needed)
./apply-store-migrations.sh
```

## Multi-Tenant Architecture

### Schema Management
The system uses **SchemaManager** for dynamic schema operations:

```java
@Inject
SchemaManager schemaManager;

// Create a new store schema with migrations
String storeSchema = "store_myshop";
schemaManager.createSchema(storeSchema);  // Creates schema + runs migrations

// Run migrations on existing schema
schemaManager.migrateSchema(storeSchema);
```

### Store Context Management
**Critical for multi-tenant operations:**

```java
// Set current store context (required for database operations)
StoreContext.setCurrentStore("store_myshop");

// Use custom annotation for automatic context setting
@RequiresStoreRole("store-admin")  // Sets store context + validates role
public void storeOperation() { /* ... */ }
```

### Migration Structure
- **Master schema**: `/src/main/resources/db/migration/master/` - Platform-level tables
- **Store schemas**: `/src/main/resources/db/migration/stores/` - Store-specific tables
- Each store maintains independent `flyway_schema_history`

## Configuration Patterns

### Key Application Properties
```properties
# Multi-tenant configuration (REQUIRED)
quarkus.hibernate-orm.multitenant=SCHEMA
quarkus.hibernate-orm.multitenant.tenant-identifier-resolver=dev.tiodati.saas.gocommerce.store.config.StoreSchemaResolver
quarkus.hibernate-orm.multitenant.tenant-connection-provider=dev.tiodati.saas.gocommerce.store.config.SchemaTenantConnectionProvider

# Store migration settings
gocommerce.flyway.store-migrations.locations=db/migration/stores
gocommerce.flyway.store-migrations.table=flyway_schema_history
gocommerce.flyway.store-migrations.baseline-version=1.0.0
```

### Environment Variables (.env)
Located in `docker/.env`:
- `DB_*`: Database connection settings
- `KEYCLOAK_*`: Authentication server configuration  
- `DB_SCHEMA_ADMIN_*`: Credentials for schema management operations

## Testing Patterns

### Multi-Tenant Testing
**Use TenantTestBase** for store-specific tests:

```java
public class MyStoreTest extends TenantTestBase {
    // Automatically creates/drops test schema per test method
    // Sets store context via StoreContext
    
    @Test
    void testStoreOperation() {
        // Test runs in isolated schema: test_store_<uuid>
    }
}
```

### Testing Strategy (Integration-First)
- **Balanced Testing Pyramid**: Mix of unit and integration tests for speed
- **Integration Tests**: `@QuarkusTest` for complete workflow testing
- **Unit Tests**: Services, mappers, validators (mocking allowed)
- **Schema Lifecycle**: Create once per test class, truncate between methods
- **Test Data**: Use `TestDataFactory` builders for consistent test data
- **Avoid Over-Mocking**: Test real integrations, mock only external services

### Test Infrastructure Classes
- `TenantTestBase`: Schema setup/teardown per test
- `TestDatabaseManager`: Centralized schema lifecycle management
- `MultiTenantTestExtension`: JUnit 5 extension for tenant context
- `TestDataFactory`: Builder pattern for test entities

### Docker-Based Testing
```bash
# Run tests with Docker dependencies
./docker/run-tests.sh integration  # Integration tests
./docker/run-tests.sh all          # All test suites
```

### Event-Driven Testing
```java
@QuarkusTest
@TestProfile(TestProfiles.Kafka.class)
public class OrderEventTest {
    @Test
    void testOrderCreatedEventFlow() {
        // Test complete event chain from order creation to processing
        // Verify event publishing, consumption, and business logic
    }
}
```

## Code Conventions

### Java Patterns (Verbosity Reduction)
```java
// DTOs as records (preferred)
public record ProductDto(UUID id, String name, BigDecimal price) {}

// Services with Lombok
@RequiredArgsConstructor @Slf4j
public class ProductService {
    private final ProductRepository repository;
}

// Fluent interfaces for builders
return ProductBuilder.newProduct()
    .withName("Product")
    .withPrice(BigDecimal.TEN)
    .build();
```

### Logging & Error Handling
```java
// Use Quarkus logging (NOT System.out.println)
io.quarkus.logging.Log.info("Creating schema: " + schemaName);

// Prefer specific exceptions over generic ones
throw new NotFoundException("Store not found: " + storeId);
```

## Package Structure

**Main feature packages under `dev.tiodati.saas.gocommerce`:**

- `platform/` - Store creation, platform administration
- `store/` - Store operations, context management  
- `product/` - Product catalog, inventory
- `customer/` - Customer management (individual/company)
- `order/` - Orders, cart, order processing
- `auth/` - Authentication, authorization, Keycloak integration
- `i18n/` - Internationalization support
- `shared/` - Cross-cutting utilities, base classes

**Each feature package typically contains:**
- `dto/` - Data transfer objects (use records)
- `resource/` - REST endpoints  
- `service/` - Business logic
- `entity/` - JPA entities (in relevant packages)
- `repository/` - Data access (Panache repositories)

## Security & Authorization

### Role-Based Access Control (RBAC)
```java
// Standard JAX-RS security
@RolesAllowed("platform-admin")
public void platformOperation() {}

// Store-specific security (custom annotation)
@RequiresStoreRole("store-admin")  // Validates role + store access + sets context
public void storeOperation() {}

// Combined approach for store endpoints
@POST
@Path("/stores/{storeId}/products")
@RolesAllowed({"platform-admin", "store-admin", "product-manager"})
@RequiresStoreRole("product-manager")
public Response createProduct(@PathParam("storeId") UUID storeId, ProductDto product) {
    // Only users with appropriate roles AND store access can proceed
}

// Programmatic checks
@Inject PermissionValidator permissionValidator;
permissionValidator.requireStoreRole("product-manager", storeId);
```

**Hierarchical Role System:**
```
platform-admin
  └── store-admin
        └── customer
```

**Specialized Store Roles:**
- `platform-admin` - Global platform access (inherits all)
- `store-admin` - Full store management (inherits customer)
- `product-manager` - Product and inventory management
- `order-manager` - Order processing and fulfillment
- `customer-service` - Customer support and order history
- `customer` - Basic store customer access

### JWT Token Structure
```json
{
  "realm_access": {
    "roles": ["store-admin", "customer"]
  },
  "storeId": "11111111-1111-1111-1111-111111111111"
}
```

## File Locations

- **Main entry point**: `README.md`
- **Architecture docs**: `/wiki/` directory
- **Docker setup**: `/docker/` (docker-compose.yml, .env, Dockerfiles)
- **Source code**: `/src/main/java/` (package-by-feature)
- **Resources**: `/src/main/resources/` (application.properties, migrations, i18n)
- **Tests**: `/src/test/java/`
- **AI context**: `.github/instructions/general-project-context.md`
- **Project rules**: `wiki/09-AI-Agent-Instructions.md`

## Common Operations

### Creating a New Store
1. Use `SchemaManager.createSchema(storeSchema)` - creates schema + runs migrations
2. Store naming convention: `"store_" + storeKey`
3. Set store context: `StoreContext.setCurrentStore(storeSchema)`
4. All subsequent database operations target the store schema

### Adding Store Migrations
1. Create SQL file in `/src/main/resources/db/migration/stores/`
2. Use Flyway naming: `V{version}__{description}.sql`
3. Migrations run automatically during schema creation
4. Each store maintains independent migration history

### Multi-Tenant Data Operations
Always ensure store context is set before database operations:
```java
// Either use annotation
@RequiresStoreRole("store-admin")  // Auto-sets context
public ResponseEntity<?> storeEndpoint() {}

// Or set manually
StoreContext.setCurrentStore(storeSchema);
// Perform database operations
StoreContext.clear(); // Clean up if needed
```

## Event-Driven Architecture

### Kafka Event Patterns
```java
// Event Producer
@Inject
@Channel("orders")
Emitter<JsonObject> orderEmitter;

public void publishOrderCreated(Order order) {
    JsonObject event = Json.createObjectBuilder()
        .add("type", "order.created")
        .add("storeId", StoreContext.getCurrentStore())
        .add("data", Json.createObjectBuilder()
            .add("orderId", order.id)
            .add("orderNumber", order.orderNumber)
        ).build();
    
    orderEmitter.send(Record.of(String.valueOf(order.id), event));
}

// Event Consumer
@Incoming("orders")
public void processOrderEvents(Message<JsonObject> message) {
    JsonObject event = message.getPayload();
    String eventType = event.getString("type");
    String storeId = event.getString("storeId");
    
    // Set store context from event
    StoreContext.setCurrentStore(storeId);
    
    switch (eventType) {
        case "order.created" -> handleOrderCreated(event);
        case "payment.completed" -> handlePaymentCompleted(event);
    }
}
```

### Key Event Types
- `order.created` - New order placed
- `order.updated` - Order status changed
- `payment.completed` - Payment processed
- `inventory.updated` - Stock levels changed
- `customer.registered` - New customer account

## Data Model Patterns

### Entity Inheritance (Customer Types)
```java
@Entity
@Table(name = "customer")
public class Customer extends PanacheEntityBase {
    @Enumerated(EnumType.STRING)
    public CustomerType type;  // INDIVIDUAL or COMPANY
    public String email;
    public String phone;
}

@Entity
@Table(name = "individual_customer")
public class IndividualCustomer extends PanacheEntityBase {
    @Id public Long customerId;
    public String firstName;
    public String lastName;
    public String documentNumber;
}

@Entity
@Table(name = "company_customer")
public class CompanyCustomer extends PanacheEntityBase {
    @Id public Long customerId;
    public String companyName;
    public String taxId;
}
```

### JSON Flexible Attributes
```java
@Entity
public class Product {
    @Column(columnDefinition = "jsonb")
    public String attributes;  // {"color": "red", "size": "M"}
    
    @Column(columnDefinition = "jsonb")
    public String dimensions;  // {"length": 10, "width": 5, "height": 2}
}

@Entity
public class Store {
    @Column(columnDefinition = "jsonb")
    public String settings;  // Theme, features, email templates
}
```

### Migration Management
```bash
# Store schema naming: store_{store_key}
# Master migrations: /src/main/resources/db/migration/master/
# Store migrations: /src/main/resources/db/migration/stores/

# Each store gets independent flyway_schema_history
```

Remember: The schema-per-store approach provides complete data isolation but requires careful context management for all database operations.
