CREATE TABLE product_kit (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE kit_item (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    kit_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_kit_item_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT fk_kit_item_kit FOREIGN KEY (kit_id) REFERENCES product_kit(id) ON DELETE CASCADE
);

-- Performance indexes
CREATE INDEX idx_product_kit_store_active ON product_kit(store_id, is_active);
CREATE INDEX idx_product_kit_name ON product_kit(name) WHERE is_active = true;
CREATE INDEX idx_kit_item_kit_id ON kit_item(kit_id);
CREATE INDEX idx_kit_item_product_id ON kit_item(product_id);

-- Unique constraint to prevent duplicate products in the same kit
CREATE UNIQUE INDEX idx_kit_item_unique ON kit_item(kit_id, product_id);
