-- Create category table for store schema
-- This table stores product categories for organizing products

CREATE TABLE category (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,

    -- Hierarchy support for nested categories
    parent_id BINARY(16),
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL,
    INDEX idx_category_slug (slug),
    INDEX idx_category_parent (parent_id),
    INDEX idx_category_active (is_active),
    INDEX idx_category_sort (sort_order)
);

-- Create product table for store schema
-- This table stores product information for each individual store

CREATE TABLE product (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    short_description VARCHAR(500),

    -- Category relationship
    category_id BINARY(16),

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
    status ENUM('DRAFT', 'ACTIVE', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    requires_shipping BOOLEAN NOT NULL DEFAULT TRUE,
    is_digital BOOLEAN NOT NULL DEFAULT FALSE,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,

    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL,
    INDEX idx_product_slug (slug),
    INDEX idx_product_category (category_id),
    INDEX idx_product_status (status),
    INDEX idx_product_sku (sku),
    INDEX idx_product_featured (is_featured),
    INDEX idx_product_price (price),
    INDEX idx_product_created_at (created_at)
);

-- Copilot: This file may have been generated or refactored by GitHub Copilot.
