-- MariaDB initialization script to grant necessary permissions for multi-schema operations
-- This script runs when MariaDB container starts for the first time

-- Grant necessary permissions to gocommerceuser for multi-schema operations
-- The user is already created by MariaDB container environment variables

-- Grant CREATE privilege to allow creating new schemas/databases
GRANT ALL ON *.* TO 'gocommerceuser'@'%';

-- Grant SELECT, INSERT, UPDATE, DELETE privileges globally for dynamic schema creation
-- This ensures Flyway can access schema history tables in dynamically created schemas
--GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON *.* TO 'gocommerceuser'@'%';

-- Grant ALL privileges on any schema that starts with 'store_'
-- This allows the application to manage tenant schemas
GRANT ALL PRIVILEGES ON `store_%`.* TO 'gocommerceuser'@'%';

-- Apply the permission changes
FLUSH PRIVILEGES;

-- Log the completion
SELECT 'Permissions granted to gocommerceuser for multi-schema operations' AS status;
