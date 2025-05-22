CREATE TABLE platform_stores (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    subdomain VARCHAR(50) NOT NULL UNIQUE,
    domain_suffix VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    keycloak_realm_id VARCHAR(100),
    database_schema VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT
);

CREATE INDEX idx_platform_stores_subdomain ON platform_stores(subdomain);
