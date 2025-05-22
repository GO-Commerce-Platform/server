CREATE TABLE platform_stores (
    id UUID PRIMARY KEY,
    subdomain VARCHAR(255) NOT NULL UNIQUE,
    domain_suffix VARCHAR(255),
    schema_name VARCHAR(255) NOT NULL UNIQUE,
    store_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    owner_id VARCHAR(255),
    keycloak_realm_id VARCHAR(255),
    configuration JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT ck_platform_stores_status CHECK (status IN ('CREATING', 'PROVISIONING', 'ACTIVE', 'SUSPENDED', 'DELETED'))
);

-- Add indexes for common query patterns
CREATE INDEX idx_platform_stores_subdomain ON platform_stores(subdomain);
CREATE INDEX idx_platform_stores_status ON platform_stores(status);
CREATE INDEX idx_platform_stores_owner_id ON platform_stores(owner_id);