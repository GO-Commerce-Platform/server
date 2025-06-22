#!/bin/bash

# Apply all store migrations to existing schemas
# This is a temporary fix for missing migrations on existing store schemas

SCHEMAS=(
    "gocommerce_success-store-a9d38778"
    "gocommerce_update-test-store-af461763"
    "gocommerce_delete-me-store-bc89571b"
)

MIGRATIONS=(
    "V3__Create_customer_table.sql"
    "V4__Create_product_and_category_tables.sql"
    "V5__Create_order_tables.sql"
    "V6__Create_cart_and_media_tables.sql"
)

for schema in "${SCHEMAS[@]}"; do
    echo "Applying migrations to schema: $schema"

    for migration in "${MIGRATIONS[@]}"; do
        echo "  Running migration: $migration"
        docker exec -i go-commerce-mariadb-1 mariadb -u gocommerceuser -pgocommercepass -D "$schema" \
            < "src/main/resources/db/migration/stores/$migration"

        if [ $? -eq 0 ]; then
            echo "    ✓ Success"
        else
            echo "    ✗ Failed"
        fi
    done

    echo "Completed schema: $schema"
    echo ""
done

echo "All migrations completed!"
