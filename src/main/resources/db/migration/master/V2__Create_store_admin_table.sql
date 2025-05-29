-- Create the store_admin table
CREATE TABLE store_admin (
    id BINARY(16) NOT NULL,
    store_id BINARY(16) NOT NULL,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    keycloak_user_id VARCHAR(255) NULL, -- Assuming this might be needed as seen in import-test.sql
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT, -- Changed from BIGINT to INT
    PRIMARY KEY (id),
    UNIQUE (username),
    UNIQUE (email),
    FOREIGN KEY (store_id) REFERENCES store(id) -- Assuming 'store' is the name of the parent table
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes
CREATE INDEX idx_store_admin_store_id ON store_admin(store_id);
CREATE INDEX idx_store_admin_keycloak_user_id ON store_admin(keycloak_user_id);

