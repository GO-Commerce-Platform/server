-- MariaDB initialization script to grant necessary permissions for multi-schema operations
-- This script runs when MariaDB container starts for the first time

-- Note: gocommerceuser is already created by MariaDB container environment variables
-- We use environment variables for secure password management in production

-- Grant ALL privileges for multi-schema operations and store management
-- This user handles both schema creation and tenant data operations
GRANT ALL PRIVILEGES ON *.* TO 'gocommerceuser'@'%' WITH GRANT OPTION;

-- Grant ALL privileges on any schema that starts with 'store_'
-- This ensures the application can manage tenant schemas dynamically
GRANT ALL PRIVILEGES ON `store_%`.* TO 'gocommerceuser'@'%';

-- Apply the permission changes
FLUSH PRIVILEGES;

-- Log the completion
SELECT 'Permissions granted to gocommerceuser for multi-schema operations' AS status;

-- Copilot: This file may have been generated or refactored by GitHub Copilot.
