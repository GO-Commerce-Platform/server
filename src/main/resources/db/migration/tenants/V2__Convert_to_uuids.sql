-- Tenant migration to convert existing tenant schemas from Long IDs to UUIDs
-- Add version fields and soft deletion flags to all entity tables

-- Convert products table
-- Create a temporary table with the new structure
CREATE TABLE IF NOT EXISTS temp_products (
    id VARCHAR(36) PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(15, 2) NOT NULL,
    cost DECIMAL(15, 2),
    stock_quantity INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    category_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create temporary mapping tables to track ID conversions
CREATE TEMPORARY TABLE category_id_mapping (
    old_id BIGINT NOT NULL PRIMARY KEY,
    new_uuid VARCHAR(36) NOT NULL
);

CREATE TEMPORARY TABLE product_id_mapping (
    old_id BIGINT NOT NULL PRIMARY KEY,
    new_uuid VARCHAR(36) NOT NULL
);

CREATE TEMPORARY TABLE customer_id_mapping (
    old_id BIGINT NOT NULL PRIMARY KEY,
    new_uuid VARCHAR(36) NOT NULL
);

CREATE TEMPORARY TABLE order_id_mapping (
    old_id BIGINT NOT NULL PRIMARY KEY,
    new_uuid VARCHAR(36) NOT NULL
);

-- Convert product_categories first (no foreign key dependencies)
-- Create temporary table
CREATE TABLE IF NOT EXISTS temp_product_categories (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id VARCHAR(36),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Copy data with new UUIDs for categories without parents
INSERT INTO temp_product_categories (id, name, description, parent_id, is_active, created_at, updated_at)
SELECT 
    UUID() as id,
    name,
    description,
    NULL, -- We'll update parent_id in a second pass
    is_active,
    created_at,
    updated_at
FROM product_categories;

-- Store ID mappings for categories
INSERT INTO category_id_mapping (old_id, new_uuid)
SELECT pc.id, tpc.id
FROM product_categories pc
JOIN temp_product_categories tpc 
ON pc.name = tpc.name AND 
   (pc.description = tpc.description OR (pc.description IS NULL AND tpc.description IS NULL));

-- Update parent_id references
UPDATE temp_product_categories tpc
JOIN product_categories pc ON pc.name = tpc.name
JOIN category_id_mapping cim ON pc.parent_id = cim.old_id
SET tpc.parent_id = cim.new_uuid
WHERE pc.parent_id IS NOT NULL;

-- Convert products table
INSERT INTO temp_products (id, sku, name, description, price, cost, stock_quantity, is_active, category_id, created_at, updated_at)
SELECT 
    UUID() as id,
    sku,
    name,
    description,
    price,
    cost,
    stock_quantity,
    is_active,
    (SELECT new_uuid FROM category_id_mapping WHERE old_id = p.category_id),
    created_at,
    updated_at
FROM products p;

-- Store product ID mappings
INSERT INTO product_id_mapping (old_id, new_uuid)
SELECT p.id, tp.id
FROM products p
JOIN temp_products tp ON p.sku = tp.sku;

-- Convert customers table
CREATE TABLE IF NOT EXISTS temp_customers (
    id VARCHAR(36) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO temp_customers (id, first_name, last_name, email, phone, address_line1, address_line2, city, state, postal_code, country, is_active, created_at, updated_at)
SELECT 
    UUID() as id,
    first_name,
    last_name,
    email,
    phone,
    address_line1,
    address_line2,
    city,
    state,
    postal_code,
    country,
    is_active,
    created_at,
    updated_at
FROM customers;

-- Store customer ID mappings
INSERT INTO customer_id_mapping (old_id, new_uuid)
SELECT c.id, tc.id
FROM customers c
JOIN temp_customers tc ON c.email = tc.email;

-- Convert orders table
CREATE TABLE IF NOT EXISTS temp_orders (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    total_amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO temp_orders (id, customer_id, order_number, total_amount, status, created_at, updated_at)
SELECT 
    UUID() as id,
    (SELECT new_uuid FROM customer_id_mapping WHERE old_id = o.customer_id),
    order_number,
    total_amount,
    status,
    created_at,
    updated_at
FROM orders o;

-- Store order ID mappings
INSERT INTO order_id_mapping (old_id, new_uuid)
SELECT o.id, to2.id
FROM orders o
JOIN temp_orders to2 ON o.order_number = to2.order_number;

-- Convert order items table
CREATE TABLE IF NOT EXISTS temp_order_items (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    total_price DECIMAL(15, 2) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0
);

INSERT INTO temp_order_items (id, order_id, product_id, quantity, unit_price, total_price)
SELECT 
    UUID() as id,
    (SELECT new_uuid FROM order_id_mapping WHERE old_id = oi.order_id),
    (SELECT new_uuid FROM product_id_mapping WHERE old_id = oi.product_id),
    quantity,
    unit_price,
    total_price
FROM order_items oi;

-- Drop foreign keys first (to avoid constraint violations)
ALTER TABLE order_items DROP FOREIGN KEY order_items_ibfk_1;
ALTER TABLE order_items DROP FOREIGN KEY order_items_ibfk_2;
ALTER TABLE orders DROP FOREIGN KEY orders_ibfk_1;
ALTER TABLE products DROP FOREIGN KEY fk_product_category;
ALTER TABLE product_categories DROP FOREIGN KEY product_categories_ibfk_1;

-- Drop original tables
DROP TABLE order_items;
DROP TABLE orders;
DROP TABLE products;
DROP TABLE customers;
DROP TABLE product_categories;

-- Rename temporary tables
ALTER TABLE temp_product_categories RENAME TO product_categories;
ALTER TABLE temp_products RENAME TO products;
ALTER TABLE temp_customers RENAME TO customers;
ALTER TABLE temp_orders RENAME TO orders;
ALTER TABLE temp_order_items RENAME TO order_items;

-- Re-create foreign keys
ALTER TABLE product_categories
ADD CONSTRAINT fk_product_category_parent 
FOREIGN KEY (parent_id) REFERENCES product_categories(id);

ALTER TABLE products
ADD CONSTRAINT fk_product_category
FOREIGN KEY (category_id) REFERENCES product_categories(id);

ALTER TABLE orders
ADD CONSTRAINT fk_orders_customer
FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE order_items
ADD CONSTRAINT fk_order_item_order
FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE order_items
ADD CONSTRAINT fk_order_item_product
FOREIGN KEY (product_id) REFERENCES products(id);

-- Recreate indexes
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);