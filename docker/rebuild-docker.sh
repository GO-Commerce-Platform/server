#!/bin/bash

# Store the script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."  # Navigate to project root

# Stop containers and remove volumes
echo "Stopping containers and removing storage..."
docker-compose --env-file "./docker/.env" down
rm -Rf storage

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

# Build and run the Docker containers
echo "Starting all Docker containers..."
docker-compose --env-file "./docker/.env" up -d

# Show logs
echo "Showing container logs (press Ctrl+C to exit logs)..."
docker-compose --env-file "./docker/.env" logs -f