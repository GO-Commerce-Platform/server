-- Create shopping cart and product media tables for store schema

CREATE TABLE shopping_cart (
    id UUID NOT NULL PRIMARY KEY,
    customer_id UUID,
    session_id VARCHAR(255), -- For anonymous users

    -- Cart metadata
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    subtotal DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days'),
    version BIGINT NOT NULL DEFAULT 0,

    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
);

-- Create indexes for shopping_cart table
CREATE INDEX idx_cart_customer ON shopping_cart (customer_id);
CREATE INDEX idx_cart_session ON shopping_cart (session_id);
CREATE INDEX idx_cart_expires ON shopping_cart (expires_at);

CREATE TABLE cart_item (
    id UUID NOT NULL PRIMARY KEY,
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,

    -- Item details
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,

    -- Timestamps
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    FOREIGN KEY (cart_id) REFERENCES shopping_cart(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    UNIQUE (cart_id, product_id)
);

-- Create indexes for cart_item table
CREATE INDEX idx_cart_item_cart ON cart_item (cart_id);
CREATE INDEX idx_cart_item_product ON cart_item (product_id);

CREATE TABLE product_image (
    id UUID NOT NULL PRIMARY KEY,
    product_id UUID NOT NULL,

    -- Image details
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),

    -- Image properties
    width INT,
    height INT,
    alt_text VARCHAR(255),

    -- Image metadata
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

-- Create indexes for product_image table
CREATE INDEX idx_image_product ON product_image (product_id);
CREATE INDEX idx_image_primary ON product_image (product_id, is_primary);
CREATE INDEX idx_image_sort ON product_image (product_id, sort_order);

-- Create ENUM type for review status
CREATE TYPE review_status_type AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE product_review (
    id UUID NOT NULL PRIMARY KEY,
    product_id UUID NOT NULL,
    customer_id UUID NOT NULL,

    -- Review content
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(255),
    review_text TEXT,

    -- Review status
    status review_status_type NOT NULL DEFAULT 'PENDING',
    is_verified_purchase BOOLEAN NOT NULL DEFAULT FALSE,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
    UNIQUE (customer_id, product_id)
);

-- Create indexes for product_review table
CREATE INDEX idx_review_product ON product_review (product_id);
CREATE INDEX idx_review_customer ON product_review (customer_id);
CREATE INDEX idx_review_rating ON product_review (product_id, rating);
CREATE INDEX idx_review_status ON product_review (status);

-- Copilot: This file may have been generated or refactored by GitHub Copilot.
