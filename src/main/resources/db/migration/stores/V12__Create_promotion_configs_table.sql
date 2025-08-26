-- Migration: Create promotion_configs table for database-backed promotions
-- This table stores tenant/store-specific promotion configurations including
-- discount codes, volume discounts, and promotional campaigns with usage tracking

CREATE TABLE promotion_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL CHECK (type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'VOLUME_BASED')),
    discount_value DECIMAL(19,4) NOT NULL CHECK (discount_value > 0),
    minimum_order_amount DECIMAL(19,2) CHECK (minimum_order_amount >= 0),
    maximum_discount_amount DECIMAL(19,2) CHECK (maximum_discount_amount >= 0),
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    usage_limit INTEGER CHECK (usage_limit > 0),
    usage_count INTEGER NOT NULL DEFAULT 0 CHECK (usage_count >= 0),
    active BOOLEAN NOT NULL DEFAULT true,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Ensure valid date range
    CONSTRAINT chk_promotion_date_range CHECK (valid_from <= valid_until),
    -- Ensure usage count doesn't exceed limit
    CONSTRAINT chk_promotion_usage CHECK (usage_limit IS NULL OR usage_count <= usage_limit),
    -- Unique promotion code per store
    CONSTRAINT uk_promotion_store_code UNIQUE (store_id, code)
);

-- Index for finding promotions by store (most common query)
CREATE INDEX idx_promotion_configs_store_id ON promotion_configs(store_id);

-- Index for finding active promotions by store
CREATE INDEX idx_promotion_configs_active_store ON promotion_configs(store_id, active)
WHERE active = true;

-- Index for finding promotions by code and store (code lookup)
CREATE INDEX idx_promotion_configs_store_code ON promotion_configs(store_id, code);

-- Index for finding promotions by type and store (volume discount queries)
CREATE INDEX idx_promotion_configs_store_type ON promotion_configs(store_id, type, active);

-- Index for finding valid promotions by date range
CREATE INDEX idx_promotion_configs_validity ON promotion_configs(valid_from, valid_until, active);

-- Index for priority-based promotion sorting
CREATE INDEX idx_promotion_configs_priority ON promotion_configs(store_id, priority DESC, minimum_order_amount DESC)
WHERE active = true;

-- Index for cleanup of expired promotions
CREATE INDEX idx_promotion_configs_expired ON promotion_configs(valid_until, active)
WHERE active = true;

-- Index for usage tracking and analytics
CREATE INDEX idx_promotion_configs_usage ON promotion_configs(store_id, usage_count, usage_limit);

-- Comments for documentation
COMMENT ON TABLE promotion_configs IS 'Store-specific promotion configurations supporting discount codes and automatic volume discounts';
COMMENT ON COLUMN promotion_configs.id IS 'Unique identifier for the promotion';
COMMENT ON COLUMN promotion_configs.store_id IS 'Store/tenant this promotion belongs to';
COMMENT ON COLUMN promotion_configs.code IS 'Promotion code (e.g., "SAVE10", "VIP20") - unique per store';
COMMENT ON COLUMN promotion_configs.name IS 'Human-readable promotion name for management UI';
COMMENT ON COLUMN promotion_configs.description IS 'Detailed description of the promotion';
COMMENT ON COLUMN promotion_configs.type IS 'Type of discount: PERCENTAGE (10% off), FIXED_AMOUNT ($5 off), VOLUME_BASED (automatic based on order total)';
COMMENT ON COLUMN promotion_configs.discount_value IS 'Discount value: percentage as decimal (0.10 for 10%) or fixed amount in base currency units';
COMMENT ON COLUMN promotion_configs.minimum_order_amount IS 'Minimum order total required to qualify for this promotion';
COMMENT ON COLUMN promotion_configs.maximum_discount_amount IS 'Maximum discount amount (caps percentage discounts)';
COMMENT ON COLUMN promotion_configs.valid_from IS 'When this promotion becomes active';
COMMENT ON COLUMN promotion_configs.valid_until IS 'When this promotion expires';
COMMENT ON COLUMN promotion_configs.usage_limit IS 'Maximum number of times this promotion can be used (NULL = unlimited)';
COMMENT ON COLUMN promotion_configs.usage_count IS 'Number of times this promotion has been used';
COMMENT ON COLUMN promotion_configs.active IS 'Whether this promotion is currently enabled';
COMMENT ON COLUMN promotion_configs.priority IS 'Priority for promotion selection (higher number = higher priority)';
COMMENT ON COLUMN promotion_configs.created_at IS 'When this promotion was created';
COMMENT ON COLUMN promotion_configs.updated_at IS 'When this promotion was last modified';
COMMENT ON COLUMN promotion_configs.version IS 'Optimistic locking version field';

-- Insert some sample promotions for development/testing
-- These can be removed or modified for production environments
INSERT INTO promotion_configs (
    id, store_id, code, name, description, type, discount_value, minimum_order_amount, 
    maximum_discount_amount, valid_from, valid_until, usage_limit, priority
) VALUES 
    -- Percentage discount with code
    (
        gen_random_uuid(), 
        '00000000-0000-0000-0000-000000000001'::uuid,
        'SAVE10', 
        '10% Off Everything', 
        'Get 10% off your entire order with code SAVE10',
        'PERCENTAGE', 
        0.10, 
        50.00, 
        25.00,
        '2024-01-01 00:00:00+00'::timestamp with time zone,
        '2024-12-31 23:59:59+00'::timestamp with time zone,
        100,
        10
    ),
    -- Fixed amount discount with code
    (
        gen_random_uuid(),
        '00000000-0000-0000-0000-000000000001'::uuid,
        'WELCOME5',
        'Welcome $5 Off',
        'New customer discount - $5 off your first order',
        'FIXED_AMOUNT',
        5.00,
        25.00,
        NULL,
        '2024-01-01 00:00:00+00'::timestamp with time zone,
        '2024-12-31 23:59:59+00'::timestamp with time zone,
        NULL,
        5
    ),
    -- Volume-based automatic discount
    (
        gen_random_uuid(),
        '00000000-0000-0000-0000-000000000001'::uuid,
        'VOLUME100',
        'Spend $100 Get 15% Off',
        'Automatic 15% discount on orders over $100',
        'VOLUME_BASED',
        0.15,
        100.00,
        50.00,
        '2024-01-01 00:00:00+00'::timestamp with time zone,
        '2024-12-31 23:59:59+00'::timestamp with time zone,
        NULL,
        15
    ),
    -- High-value volume discount
    (
        gen_random_uuid(),
        '00000000-0000-0000-0000-000000000001'::uuid,
        'VOLUME500',
        'Spend $500 Get 20% Off',
        'Automatic 20% discount on orders over $500',
        'VOLUME_BASED',
        0.20,
        500.00,
        100.00,
        '2024-01-01 00:00:00+00'::timestamp with time zone,
        '2024-12-31 23:59:59+00'::timestamp with time zone,
        NULL,
        20
    );

-- Create a trigger to automatically update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_promotion_configs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = COALESCE(OLD.version, 0) + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_promotion_configs_updated_at
    BEFORE UPDATE ON promotion_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_promotion_configs_updated_at();
