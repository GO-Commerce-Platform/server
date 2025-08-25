-- Migration: Create stock_reservation table for persistent stock reservations
-- This table supports database-backed stock reservations that survive server restarts
-- and work correctly in multi-instance deployments with automatic expiry cleanup

CREATE TABLE stock_reservation (
    reservation_id VARCHAR(255) PRIMARY KEY,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'CONFIRMED', 'RELEASED', 'EXPIRED')),
    reserved_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reserved_by VARCHAR(255),
    reference VARCHAR(255),
    notes TEXT,
    updated_at TIMESTAMP WITH TIME ZONE,
    
    -- Foreign key to ensure product exists
    CONSTRAINT fk_stock_reservation_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

-- Index for finding reservations by product (for availability calculations)
CREATE INDEX idx_stock_reservation_product_id ON stock_reservation(product_id);

-- Index for finding active reservations
CREATE INDEX idx_stock_reservation_status ON stock_reservation(status);

-- Index for expiry cleanup queries
CREATE INDEX idx_stock_reservation_expires_at ON stock_reservation(expires_at);

-- Composite index for finding active reservations by product
CREATE INDEX idx_stock_reservation_active_product ON stock_reservation(product_id, status, expires_at)
WHERE status = 'ACTIVE';

-- Index for finding reservations by user/system
CREATE INDEX idx_stock_reservation_reserved_by ON stock_reservation(reserved_by);

-- Index for finding reservations by reference (order ID, session ID, etc.)
CREATE INDEX idx_stock_reservation_reference ON stock_reservation(reference);

-- Comments for documentation
COMMENT ON TABLE stock_reservation IS 'Tracks temporary stock reservations with automatic expiry for order processing';
COMMENT ON COLUMN stock_reservation.reservation_id IS 'Unique identifier for the reservation (typically order ID or session ID)';
COMMENT ON COLUMN stock_reservation.product_id IS 'Product being reserved';
COMMENT ON COLUMN stock_reservation.quantity IS 'Quantity of stock reserved (must be positive)';
COMMENT ON COLUMN stock_reservation.status IS 'Current status: ACTIVE (blocking stock), CONFIRMED (converted to sale), RELEASED (canceled), EXPIRED (timed out)';
COMMENT ON COLUMN stock_reservation.reserved_at IS 'When the reservation was created';
COMMENT ON COLUMN stock_reservation.expires_at IS 'When the reservation expires and can be cleaned up';
COMMENT ON COLUMN stock_reservation.reserved_by IS 'User or system that created the reservation';
COMMENT ON COLUMN stock_reservation.reference IS 'Optional reference to order, session, or other business context';
COMMENT ON COLUMN stock_reservation.notes IS 'Additional notes about the reservation';
COMMENT ON COLUMN stock_reservation.updated_at IS 'When the reservation was last modified';
