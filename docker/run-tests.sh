#!/bin/bash

# Store the script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."  # Navigate to project root

# Check if Docker services are running
if ! docker ps | grep -q "gocommerce-mariadb"; then
    echo "Docker services are not running. Starting required services..."
    docker-compose --env-file "./docker/.env" up -d mariadb keycloak-db keycloak
    
    # Wait for services to be ready
    echo "Waiting for MariaDB to be ready..."
    while ! docker-compose --env-file "./docker/.env" exec mariadb mariadb-admin ping -h localhost -u root -p${DB_ROOT_PASSWORD:-rootpassword} --silent; do
        echo "Waiting for MariaDB to be ready..."
        sleep 2
    done
    echo "MariaDB is ready."
fi

# Run tests
echo "Running tests..."
if [ "$1" == "all" ]; then
    mvn test
elif [ "$1" == "integration" ]; then
    mvn verify
else
    # Default to simple tests
    mvn test
fi