-- Create order and order_item tables for store schema
-- These tables handle customer orders and line items

CREATE TABLE order_status (
    id VARCHAR(20) NOT NULL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0
);

-- Insert default order statuses
INSERT INTO order_status (id, name, description, sort_order) VALUES
('PENDING', 'Pending', 'Order placed but not yet processed', 10),
('CONFIRMED', 'Confirmed', 'Order confirmed and ready for processing', 20),
('PROCESSING', 'Processing', 'Order is being prepared', 30),
('SHIPPED', 'Shipped', 'Order has been shipped', 40),
('DELIVERED', 'Delivered', 'Order has been delivered', 50),
('CANCELLED', 'Cancelled', 'Order has been cancelled', 60),
('REFUNDED', 'Refunded', 'Order has been refunded', 70);

CREATE TABLE order_header (
    id UUID NOT NULL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- Order totals
    subtotal DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    shipping_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,

    -- Currency and locale
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    locale VARCHAR(5) DEFAULT 'en',

    -- Shipping address
    shipping_first_name VARCHAR(100),
    shipping_last_name VARCHAR(100),
    shipping_address_line1 VARCHAR(255),
    shipping_address_line2 VARCHAR(255),
    shipping_city VARCHAR(100),
    shipping_state_province VARCHAR(100),
    shipping_postal_code VARCHAR(20),
    shipping_country VARCHAR(2),
    shipping_phone VARCHAR(20),

    -- Billing address
    billing_first_name VARCHAR(100),
    billing_last_name VARCHAR(100),
    billing_address_line1 VARCHAR(255),
    billing_address_line2 VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state_province VARCHAR(100),
    billing_postal_code VARCHAR(20),
    billing_country VARCHAR(2),
    billing_phone VARCHAR(20),

    -- Order dates and notes
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    shipped_date TIMESTAMP NULL,
    delivered_date TIMESTAMP NULL,
    notes TEXT,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    FOREIGN KEY (customer_id) REFERENCES customer(id),
    FOREIGN KEY (status) REFERENCES order_status(id)
);

-- Create indexes for order_header table
CREATE INDEX idx_order_customer ON order_header (customer_id);
CREATE INDEX idx_order_status ON order_header (status);
CREATE INDEX idx_order_number ON order_header (order_number);
CREATE INDEX idx_order_date ON order_header (order_date);
CREATE INDEX idx_order_total ON order_header (total_amount);

CREATE TABLE order_item (
    id UUID NOT NULL PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,

    -- Product details at time of order (for historical accuracy)
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100),

    -- Pricing and quantity
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,

    -- Product attributes at time of order
    product_weight DECIMAL(8, 3),
    product_dimensions VARCHAR(50), -- "L x W x H" format

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    FOREIGN KEY (order_id) REFERENCES order_header(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(id)
);

-- Create indexes for order_item table
CREATE INDEX idx_order_item_order ON order_item (order_id);
CREATE INDEX idx_order_item_product ON order_item (product_id);
CREATE INDEX idx_order_item_sku ON order_item (product_sku);

-- Copilot: This file may have been generated or refactored by GitHub Copilot.
