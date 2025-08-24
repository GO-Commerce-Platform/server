-- Migration: Create inventory_adjustment table for tracking inventory changes
-- This table provides complete audit trail for all inventory adjustments

CREATE TABLE inventory_adjustment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    adjustment_type VARCHAR(20) NOT NULL CHECK (adjustment_type IN ('INCREASE', 'DECREASE', 'SET')),
    quantity INTEGER NOT NULL,
    previous_quantity INTEGER NOT NULL,
    new_quantity INTEGER NOT NULL,
    reason VARCHAR(255) NOT NULL,
    reference VARCHAR(100),
    notes TEXT,
    adjusted_by VARCHAR(255),
    adjusted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for common queries
    CONSTRAINT fk_inventory_adjustment_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

-- Index for querying adjustments by product
CREATE INDEX idx_inventory_adjustment_product_id ON inventory_adjustment(product_id);

-- Index for querying adjustments by date
CREATE INDEX idx_inventory_adjustment_date ON inventory_adjustment(adjusted_at);

-- Index for querying adjustments by type
CREATE INDEX idx_inventory_adjustment_type ON inventory_adjustment(adjustment_type);

-- Index for querying recent adjustments
CREATE INDEX idx_inventory_adjustment_recent ON inventory_adjustment(adjusted_at DESC, product_id);

-- Comments for documentation
COMMENT ON TABLE inventory_adjustment IS 'Tracks all inventory adjustments with complete audit trail';
COMMENT ON COLUMN inventory_adjustment.adjustment_type IS 'Type of adjustment: INCREASE (add stock), DECREASE (remove stock), SET (set to absolute value)';
COMMENT ON COLUMN inventory_adjustment.quantity IS 'Amount of the adjustment (always positive, direction determined by type)';
COMMENT ON COLUMN inventory_adjustment.previous_quantity IS 'Stock quantity before the adjustment';
COMMENT ON COLUMN inventory_adjustment.new_quantity IS 'Stock quantity after the adjustment';
COMMENT ON COLUMN inventory_adjustment.reason IS 'Business reason for the adjustment';
COMMENT ON COLUMN inventory_adjustment.reference IS 'Optional reference number (PO, transfer, etc.)';
COMMENT ON COLUMN inventory_adjustment.adjusted_by IS 'User who performed the adjustment';
