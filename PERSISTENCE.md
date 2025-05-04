# E-Commerce Persistence Specification

This document outlines the database structure and persistence approach for the e-commerce API service.

## Database Technology

The application uses MariaDB as the primary database with the following characteristics:

- **RDBMS:** MariaDB (Version 11.2 used in `docker-compose.yml`)
- **Schema Flexibility:** JSON column support for varying product attributes
- **Transaction Support:** ACID compliance for reliable order processing
- **ORM Layer:** Hibernate ORM with Panache
- **Multi-Tenancy:** Schema-per-tenant approach for SaaS/marketplace model

## Multi-Tenancy Support

The system is designed as a multi-tenant application where each tenant represents a separate store in the marketplace. We implement a schema-per-tenant approach with the following characteristics:

### Tenant Management

```sql
CREATE TABLE tenant (
    id VARCHAR(36) PRIMARY KEY,
    tenant_key VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100) NOT NULL UNIQUE,
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'TRIAL') NOT NULL DEFAULT 'TRIAL',
    schema_name VARCHAR(50) NOT NULL UNIQUE,
    billing_plan VARCHAR(50) NOT NULL DEFAULT 'BASIC',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    settings JSON
);

CREATE TABLE tenant_admin (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tenant_admin_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);
```

### Multi-Tenant Schema Approach

All tenant-specific data is stored in separate schemas named according to the tenant's `schema_name`. Each schema contains identical table structures but isolated data. The main schema contains global tables like `tenant` and `tenant_admin`.

### Tenant-Specific Schema

For each tenant, the system creates a separate schema with all the e-commerce tables. These schemas follow a naming convention like `tenant_{tenant_key}` and contain all tables defined in the "Database Schema" section below.

### Tenant-Specific Settings

Each tenant has their own settings stored in the JSON `settings` column, which may include:

```json
{
  "theme": {
    "primaryColor": "#FF5722",
    "secondaryColor": "#2196F3",
    "logo": "https://storage.example.com/tenant123/logo.png"
  },
  "features": {
    "enableReviews": true,
    "enableWishlist": true,
    "maxProductsAllowed": 10000,
    "enableMultiCurrency": false
  },
  "email": {
    "senderName": "My Store",
    "senderEmail": "sales@tenant-store.com",
    "templates": {
      "orderConfirmation": "template-1",
      "welcomeEmail": "template-2"
    }
  },
  "payment": {
    "providers": ["stripe", "paypal"],
    "stripeAccountId": "acct_123456"
  }
}
```

## Database Schema

### Core Entities

#### Customer

```sql
CREATE TABLE customer (
    id VARCHAR(36) PRIMARY KEY,
    type ENUM('INDIVIDUAL', 'COMPANY') NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    phone VARCHAR(20),
    preferences JSON
);

CREATE TABLE individual_customer (
    customer_id VARCHAR(36) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_number VARCHAR(20) NOT NULL UNIQUE,
    birth_date DATE,
    CONSTRAINT fk_individual_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
);

CREATE TABLE company_customer (
    customer_id VARCHAR(36) PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    tax_id VARCHAR(30) NOT NULL UNIQUE,
    incorporation_date DATE,
    business_segment VARCHAR(100),
    CONSTRAINT fk_company_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
);
```

#### Address

```sql
CREATE TABLE address (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    name VARCHAR(50) NOT NULL,
    recipient VARCHAR(150) NOT NULL,
    street VARCHAR(255) NOT NULL,
    number VARCHAR(20),
    complement VARCHAR(100),
    neighborhood VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_billing BOOLEAN NOT NULL DEFAULT FALSE,
    is_shipping BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_address_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
);
```

#### Product

```sql
CREATE TABLE product_category (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_id VARCHAR(36),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES product_category(id) ON DELETE CASCADE
);

CREATE TABLE product (
    id VARCHAR(36) PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    short_description VARCHAR(255),
    type ENUM('PHYSICAL', 'DIGITAL', 'SERVICE') NOT NULL,
    weight DECIMAL(10,2),
    dimensions JSON, -- {"length": 10, "width": 5, "height": 2} in cm
    attributes JSON, -- Flexible attributes schema
    category_id VARCHAR(36),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_sku UNIQUE (sku),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category(id)
);

CREATE TABLE product_inventory (
    product_id VARCHAR(36) PRIMARY KEY,
    quantity_available INT NOT NULL DEFAULT 0,
    reorder_level INT NOT NULL DEFAULT 5,
    last_restock_date TIMESTAMP,
    reserved_quantity INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);
```

#### Pricing

```sql
CREATE TABLE price_list (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    priority INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    customer_type ENUM('ALL', 'INDIVIDUAL', 'COMPANY') NOT NULL DEFAULT 'ALL',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE product_price (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    price_list_id VARCHAR(36) NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    sale_price DECIMAL(10,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_product_price UNIQUE (product_id, price_list_id),
    CONSTRAINT fk_price_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT fk_price_list FOREIGN KEY (price_list_id) REFERENCES price_list(id) ON DELETE CASCADE
);
```

#### Kits/Combos

```sql
CREATE TABLE product_kit (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sku VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    discount_type ENUM('PERCENTAGE', 'FIXED') NOT NULL DEFAULT 'PERCENTAGE',
    discount_value DECIMAL(10,2) NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE product_kit_item (
    kit_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    CONSTRAINT pk_kit_item PRIMARY KEY (kit_id, product_id),
    CONSTRAINT fk_kit_item_kit FOREIGN KEY (kit_id) REFERENCES product_kit(id) ON DELETE CASCADE,
    CONSTRAINT fk_kit_item_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);
```

#### Order

```sql
CREATE TABLE customer_order (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status ENUM('DRAFT', 'PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELED', 'RETURNED') NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    shipping_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    billing_address_id VARCHAR(36),
    shipping_address_id VARCHAR(36),
    payment_method VARCHAR(50),
    payment_status ENUM('PENDING', 'AUTHORIZED', 'PAID', 'REFUNDED', 'FAILED') NOT NULL,
    notes TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_number UNIQUE (order_number),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_order_billing_address FOREIGN KEY (billing_address_id) REFERENCES address(id),
    CONSTRAINT fk_order_shipping_address FOREIGN KEY (shipping_address_id) REFERENCES address(id)
);

CREATE TABLE order_item (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36),
    kit_id VARCHAR(36),
    product_name VARCHAR(255) NOT NULL, -- Denormalized for historical record
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_price DECIMAL(10,2) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES customer_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_order_item_kit FOREIGN KEY (kit_id) REFERENCES product_kit(id),
    CONSTRAINT check_product_or_kit CHECK (
        (product_id IS NOT NULL AND kit_id IS NULL) OR 
        (product_id IS NULL AND kit_id IS NOT NULL)
    )
);
```

## Hibernate Entity Mapping

The database schema will be implemented as JPA entities using Hibernate ORM with Panache.

### Multi-Tenant Entity Example (e.g., Product)

```java
package dev.tiodati.saas.gocommerce.model; // Correct package

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product")
public class Product extends PanacheEntityBase {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id; // Using UUID instead of Long

    // Tenant identity is managed by the schema

    @Column(nullable = false, unique = true)
    public String sku;

    @Column(nullable = false)
    public String name;

    @Column(length = 2000)
    public String description;

    @Column(name = "short_description")
    public String shortDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ProductType type; // Assuming ProductType enum exists

    public BigDecimal weight;

    @Column(columnDefinition = "json")
    public String dimensions;

    @Column(columnDefinition = "json")
    public String attributes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    public ProductCategory category; // Assuming ProductCategory entity exists

    @Column(nullable = false)
    public boolean active = true;
    
    @Column(name = "is_deleted", nullable = false)
    public boolean isDeleted = false;
    
    @Version
    public int version;

    @Column(name = "created_at", updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    // Getters/Setters are often omitted when using Panache public fields
    // or generated by Lombok if used.
}
```

### Tenant Entity (Shared Schema)

```java
package dev.tiodati.saas.gocommerce.model; // Correct package

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant") // This table resides in the default/shared schema
public class Tenant extends PanacheEntityBase {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "tenant_key", nullable = false, unique = true)
    public String tenantKey;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false, unique = true)
    public String subdomain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TenantStatus status; // Assuming TenantStatus enum exists

    @Column(name = "schema_name", nullable = false, unique = true)
    public String schemaName;

    @Column(name = "billing_plan", nullable = false)
    public String billingPlan;
    
    @Column(name = "is_deleted", nullable = false)
    public boolean isDeleted = false;
    
    @Version
    public int version;

    @Column(columnDefinition = "json")
    public String settings;

    @Column(name = "created_at", updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    // Getters/Setters omitted for brevity or handled by Panache/Lombok
}
```

## JSON Usage

JSON columns will be used for flexibility in the following areas:

1. **Product Attributes**: Storing varied attributes for different product types
   ```json
   {
     "color": "red",
     "size": "XL",
     "material": "cotton",
     "features": ["waterproof", "breathable"]
   }
   ```

2. **Customer Preferences**: For storing customer-specific settings
   ```json
   {
     "marketingEmails": true,
     "favoriteCategories": [1, 4, 7],
     "uiPreferences": {
       "theme": "dark",
       "resultsPerPage": 20
     }
   }
   ```

3. **Product Dimensions**: For physical products
   ```json
   {
     "length": 10.5,
     "width": 5.2,
     "height": 2.0,
     "unit": "cm"
   }
   ```

## Database Configuration

The database connection is configured in `application.properties`, utilizing `.env` variables via `quarkus-dotenv`.

```properties
# Database Configuration (Defaults shown, actual values may come from .env)
quarkus.datasource.db-kind=mariadb
quarkus.datasource.username=${DB_USERNAME:gocommerceuser}
quarkus.datasource.password=${DB_PASSWORD:gocommercepass}
quarkus.datasource.jdbc.url=jdbc:mariadb://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:gocommerce}
quarkus.datasource.jdbc.max-size=16
quarkus.datasource.jdbc.min-size=4
quarkus.datasource.jdbc.idle-timeout=PT1M

# Hibernate ORM
quarkus.hibernate-orm.database.generation=none # Use 'validate' or 'none' with Flyway
quarkus.hibernate-orm.log.sql=true
# quarkus.hibernate-orm.sql-load-script=import.sql # Optional seeding

# Multi-tenant specific configuration
quarkus.hibernate-orm.multitenant=SCHEMA
quarkus.hibernate-orm.persistence-units.gocommerce.packages=dev.tiodati.saas.gocommerce.model # Correct package
# quarkus.hibernate-orm.database.default-tenant=public # Optional default tenant ID

```
*Note: `quarkus.hibernate-orm.database.generation` should typically be `validate` or `none` when using Flyway for schema management.*

## Tenant Resolver

A custom tenant resolver implementation (`dev.tiodati.saas.gocommerce.tenant.RequestPathTenantResolver`) is used to determine the current tenant schema based on the incoming request.

The current implementation attempts to resolve the tenant ID from a path parameter named `tenantId`. If not found, it falls back to a default tenant ID (configured via `quarkus.hibernate-orm.database.default-tenant`, defaulting to "public").

This logic might need adjustment based on the final multi-tenancy strategy (e.g., using subdomains, JWT claims, or headers).

## Migrations

Database migrations are managed using Flyway.

```properties
# Flyway Migration Configuration
quarkus.flyway.migrate-at-start=true
quarkus.flyway.baseline-on-migrate=true
quarkus.flyway.baseline-version=0.0.0
quarkus.flyway.schemas=${DB_NAME:gocommerce} # Default schema for the main Flyway history table
quarkus.flyway.table=flyway_schema_history # Table in the default schema
```

-   Flyway core and MySQL/MariaDB dependencies are explicitly managed in `pom.xml` (currently version 10.15.0) for compatibility.
-   Migration scripts for the *tenant schema structure* are placed in `src/main/resources/db/migration` (e.g., `V1__create_tables.sql`).
-   Applying migrations to *newly created tenant schemas* needs to be handled programmatically, typically when a new `Tenant` entity is created. This involves invoking Flyway manually against the new tenant's schema.
-   The `quarkus.flyway.schemas` property defines where the main `flyway_schema_history` table lives (usually the default/public schema), not the tenant schemas themselves.

## Transactions

Transactions will be managed using the `@Transactional` annotation on service methods to ensure data consistency, especially for order processing and inventory management operations. Transaction boundaries will respect tenant isolation.