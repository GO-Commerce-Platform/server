#!/bin/bash

# Store the script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."  # Navigate to project root

# Start database services for tests
echo "Starting database services required for tests..."
docker-compose --env-file "./docker/.env" up -d mariadb keycloak-db keycloak

# Wait for MariaDB to be ready
echo "Waiting for MariaDB to be ready..."
while ! docker-compose --env-file "./docker/.env" exec mariadb mariadb-admin ping -h localhost -u root -p${DB_ROOT_PASSWORD:-rootpassword} --silent 2>/dev/null; do
    echo "Waiting for MariaDB to be ready..."
    sleep 2
done
echo "MariaDB is ready."

# Build the Quarkus application
echo "Building Quarkus application..."
mvn package

# Start the rest of the containers if not already running
echo "Starting all Docker containers..."
docker-compose --env-file "./docker/.env" up -d

echo "Services are starting:"
echo "- MariaDB: localhost:${DB_PORT:-3306}"
echo "- Keycloak: http://localhost:${KEYCLOAK_PORT:-9000}"
echo "- GoCommerce App: http://localhost:${APP_PORT:-8080}"
echo ""
echo "Use 'docker-compose logs -f <service>' to view service logs"