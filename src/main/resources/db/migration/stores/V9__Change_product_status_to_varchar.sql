-- Change product status column from enum to varchar to fix Hibernate compatibility
-- This migration converts the product_status_type enum column to a varchar column

-- First, alter the column to use varchar instead of the enum type
ALTER TABLE product 
ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

-- Drop the enum type since it's no longer needed
-- Note: This will fail if any other tables are using this enum type
-- For now, we'll comment this out to avoid breaking other potential uses
-- DROP TYPE IF EXISTS product_status_type;

-- Add a check constraint to ensure only valid status values are allowed
ALTER TABLE product 
ADD CONSTRAINT product_status_check 
CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'));

-- Copilot: This file may have been generated or refactored by GitHub Copilot.
