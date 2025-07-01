-- Create ENUM type for store status
CREATE TYPE store_status_type AS ENUM ('PENDING', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'CLOSED', 'ARCHIVED', 'DELETING', 'DELETED');

CREATE TABLE store (
    id UUID NOT NULL,
    store_key VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(255) NOT NULL UNIQUE,
    domain_suffix VARCHAR(255),
    description TEXT,
    email VARCHAR(255) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    default_locale VARCHAR(10) NOT NULL,
    status store_status_type NOT NULL DEFAULT 'PENDING',
    schema_name VARCHAR(255) NOT NULL UNIQUE,
    billing_plan VARCHAR(255) NOT NULL DEFAULT 'BASIC',
    owner_id VARCHAR(255),
    keycloak_realm_id VARCHAR(255),
    settings JSONB,
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- Optional: Add indexes for frequently queried columns
CREATE INDEX idx_store_status ON store (status);
CREATE INDEX idx_store_email ON store (email);
CREATE INDEX idx_store_owner_id ON store (owner_id);
