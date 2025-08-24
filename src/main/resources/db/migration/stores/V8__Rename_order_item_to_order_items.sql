-- Rename order_item table to order_items to match entity annotation
-- This fixes the table name mismatch between entity and database

ALTER TABLE order_item RENAME TO order_items;

-- Copilot: This file may have been generated or refactored by GitHub Copilot.
