-- Test data for database
-- This script is executed during test setup when using the KeycloakTestProfile

-- Test stores
INSERT INTO store (id, store_key, name, subdomain, status, schema_name, billing_plan, created_at, updated_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'test-store', 'Test Store', 'teststore', 'TRIAL', 'store_test_store', 'BASIC', NOW(), NOW());

-- Test store admins
INSERT INTO store_admin (id, store_id, username, email, first_name, last_name, keycloak_user_id, status, created_at, updated_at)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'testadmin', 'testadmin@example.com', 'Test', 'Admin', '33333333-3333-3333-3333-333333333333', 'ACTIVE', NOW(), NOW());
