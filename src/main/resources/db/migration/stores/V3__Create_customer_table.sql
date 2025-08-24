-- Create customer table for store schema
-- This table stores customer information for each individual store

-- Create ENUM types for PostgreSQL
CREATE TYPE gender_type AS ENUM ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY');
CREATE TYPE customer_status_type AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');

CREATE TABLE customer (
    id UUID NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE,
    gender gender_type,

    -- Address information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2), -- ISO 3166-1 alpha-2 country code

    -- Account status and preferences
    status customer_status_type NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_emails_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    preferred_language VARCHAR(5) DEFAULT 'en', -- ISO 639-1 language code

    -- Authentication integration
    keycloak_user_id VARCHAR(255) UNIQUE, -- Links customer profile with Keycloak authentication account

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_customer_email ON customer (email);
CREATE INDEX idx_customer_status ON customer (status);
CREATE INDEX idx_customer_created_at ON customer (created_at);
CREATE INDEX idx_customer_keycloak_user_id ON customer (keycloak_user_id);

-- Copilot: This file may have been generated or refactored by GitHub Copilot.
