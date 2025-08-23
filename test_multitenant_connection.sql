-- Simple test to check which schema is being accessed
-- This will help us understand if the multi-tenancy is working at the SQL level

-- Test 1: Check current schema
SELECT current_schema();

-- Test 2: Check search_path  
SHOW search_path;

-- Test 3: List available schemas
SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'test_%';

-- Test 4: Check if tenant tables exist in any test schema
SELECT schemaname, tablename 
FROM pg_tables 
WHERE schemaname LIKE 'test_%' 
AND tablename = 'customer' 
ORDER BY schemaname;
