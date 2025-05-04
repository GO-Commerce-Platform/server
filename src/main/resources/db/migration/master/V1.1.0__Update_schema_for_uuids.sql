-- Migration to convert master database IDs from Long to UUID, and add soft deletion and versioning

-- First, drop any leftover temporary tables from previous migration attempts
DROP TABLE IF EXISTS temp_tenant_admin;
DROP TABLE IF EXISTS temp_tenant;
DROP TEMPORARY TABLE IF EXISTS id_mapping;

-- Create temporary tables with the new UUID structure
CREATE TABLE temp_tenant (
    id VARCHAR(36) PRIMARY KEY,
    tenant_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'TRIAL',
    schema_name VARCHAR(100) NOT NULL UNIQUE,
    billing_plan VARCHAR(50) NOT NULL DEFAULT 'BASIC',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    settings JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_key (tenant_key),
    INDEX idx_subdomain (subdomain)
);

CREATE TABLE temp_tenant_admin (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email)
);

-- Check if the tenant table exists before trying to migrate data
SET @tenant_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tenant');

-- Only run data migration if the original tables exist
SET @continue_migration = @tenant_exists;

-- Copy data from old tables to new tables, generating UUIDs for IDs
-- Only if the source table exists
DELIMITER //
CREATE PROCEDURE migrate_data()
BEGIN
    IF @continue_migration > 0 THEN
        -- Copy tenant data
        INSERT INTO temp_tenant (id, tenant_key, name, subdomain, status, schema_name, billing_plan, settings, created_at, updated_at)
        SELECT 
            UUID() as id,
            tenant_key,
            name,
            subdomain,
            status,
            schema_name,
            billing_plan,
            settings,
            created_at,
            updated_at
        FROM tenant;

        -- Create a temporary mapping table to store old IDs to new UUIDs
        CREATE TEMPORARY TABLE id_mapping (
            old_id BIGINT NOT NULL PRIMARY KEY,
            new_uuid VARCHAR(36) NOT NULL
        );

        -- Insert mapping entries
        INSERT INTO id_mapping (old_id, new_uuid)
        SELECT t.id, tt.id
        FROM tenant t
        JOIN temp_tenant tt ON t.tenant_key = tt.tenant_key;

        -- Check if tenant_admin table exists
        SET @tenant_admin_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'tenant_admin');
        
        -- Copy tenant admin data using the mapping table if it exists
        IF @tenant_admin_exists > 0 THEN
            INSERT INTO temp_tenant_admin (id, tenant_id, email, password_hash, first_name, last_name, status, created_at, updated_at)
            SELECT 
                UUID() as id,
                (SELECT new_uuid FROM id_mapping WHERE old_id = ta.tenant_id) as tenant_id,
                email,
                password_hash,
                first_name,
                last_name,
                status,
                created_at,
                updated_at
            FROM tenant_admin ta;
        END IF;

        -- Check for foreign key existence before trying to drop it
        SET @fk_exists = (
            SELECT COUNT(*) FROM information_schema.key_column_usage 
            WHERE constraint_name = 'tenant_admin_ibfk_1' 
            AND table_schema = DATABASE()
        );
        
        -- Drop the constraints and original tables, only if they exist
        IF @fk_exists > 0 THEN
            SET foreign_key_checks = 0;
            ALTER TABLE tenant_admin DROP FOREIGN KEY tenant_admin_ibfk_1;
            SET foreign_key_checks = 1;
        END IF;
        
        -- Drop the original tables
        IF @tenant_admin_exists > 0 THEN
            DROP TABLE tenant_admin;
        END IF;
        IF @tenant_exists > 0 THEN
            DROP TABLE tenant;
        END IF;
    END IF;
END //
DELIMITER ;

-- Execute the procedure
CALL migrate_data();
DROP PROCEDURE IF EXISTS migrate_data;

-- Rename temporary tables to original names - this should work regardless
-- of whether we did the data migration or not
RENAME TABLE temp_tenant TO tenant;
RENAME TABLE temp_tenant_admin TO tenant_admin;

-- Add foreign key constraint back
ALTER TABLE tenant_admin
ADD CONSTRAINT fk_tenant_admin_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE;