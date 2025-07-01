-- Create category table for store schema
-- This table stores product categories for organizing products

CREATE TABLE category (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,

    -- Hierarchy support for nested categories
    parent_id UUID,
    sort_order INT NOT NULL DEFAULT 0,

    -- SEO and metadata
    meta_title VARCHAR(255),
    meta_description TEXT,
    meta_keywords VARCHAR(500),

    -- Visibility and status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL
);

-- Create indexes for category table
CREATE INDEX idx_category_slug ON category (slug);
CREATE INDEX idx_category_parent ON category (parent_id);
CREATE INDEX idx_category_active ON category (is_active);
CREATE INDEX idx_category_sort ON category (sort_order);

-- Create ENUM type for product status
CREATE TYPE product_status_type AS ENUM ('DRAFT', 'ACTIVE', 'ARCHIVED');

-- Create product table for store schema
-- This table stores product information for each individual store

CREATE TABLE product (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    short_description VARCHAR(500),

    -- Category relationship
    category_id UUID,

    -- Pricing
    price DECIMAL(10, 2) NOT NULL,
    compare_at_price DECIMAL(10, 2), -- For showing discounts
    cost_price DECIMAL(10, 2), -- For profit calculations

    -- Inventory
    sku VARCHAR(100) UNIQUE,
    barcode VARCHAR(100),
    track_inventory BOOLEAN NOT NULL DEFAULT TRUE,
    inventory_quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT DEFAULT 10,

    -- Physical attributes
    weight DECIMAL(8, 3), -- in kg
    length DECIMAL(8, 2), -- in cm
    width DECIMAL(8, 2),  -- in cm
    height DECIMAL(8, 2), -- in cm

    -- SEO and metadata
    meta_title VARCHAR(255),
    meta_description TEXT,
    meta_keywords VARCHAR(500),

    -- Product status and visibility
    status product_status_type NOT NULL DEFAULT 'DRAFT',
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    requires_shipping BOOLEAN NOT NULL DEFAULT TRUE,
    is_digital BOOLEAN NOT NULL DEFAULT FALSE,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL
);

-- Create indexes for product table
CREATE INDEX idx_product_slug ON product (slug);
CREATE INDEX idx_product_category ON product (category_id);
CREATE INDEX idx_product_status ON product (status);
CREATE INDEX idx_product_sku ON product (sku);
CREATE INDEX idx_product_featured ON product (is_featured);
CREATE INDEX idx_product_price ON product (price);
CREATE INDEX idx_product_created_at ON product (created_at);

-- Copilot: This file may have been generated or refactored by GitHub Copilot.
