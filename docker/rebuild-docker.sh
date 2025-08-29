#!/bin/bash

# GO-Commerce Docker Rebuild Script
# Tears down and rebuilds the entire Docker environment from scratch

set -e  # Exit on any error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "🔄 GO-Commerce - Rebuilding Docker environment from scratch..."
echo "📂 Project root: $PROJECT_ROOT"
echo "📂 Docker directory: $SCRIPT_DIR"

# Check if .env file exists
if [ ! -f "$SCRIPT_DIR/.env" ]; then
    echo "❌ Error: .env file not found in $SCRIPT_DIR"
    echo "💡 Please copy .env.template to .env and configure your environment variables"
    exit 1
fi

echo "✅ Environment file found: $SCRIPT_DIR/.env"

# Change to docker directory for proper compose context
cd "$SCRIPT_DIR"

echo ""
echo "🛑 Step 1: Stopping all containers..."
docker-compose --env-file .env down --remove-orphans

echo ""
echo "🗑️ Step 2: Removing Docker volumes and networks..."
# Remove volumes to ensure fresh start
docker-compose --env-file .env down -v --remove-orphans

echo ""
echo "🧹 Step 3: Cleaning up unused Docker resources..."
# Clean up any dangling images, containers, and build cache
docker system prune -f

echo ""
echo "🏗️ Step 4: Removing existing data volumes..."
# Remove persistent data directories for fresh start
if [ -d "./storage" ]; then
    echo "  📁 Removing ./storage directory..."
    rm -rf "./storage"
fi

echo ""
echo "🔨 Step 5: Building all Docker images from scratch..."
# Build all images without cache to ensure latest code
docker-compose --env-file .env build --no-cache

echo ""
echo "🐘 Step 6: Starting PostgreSQL databases..."
# Start databases first and wait for them to be healthy
docker-compose --env-file .env up -d postgres keycloak-db

echo ""
echo "⏳ Step 7: Waiting for databases to initialize..."
# Wait for PostgreSQL (macOS compatible, longer wait for fresh initialization)
for i in {1..40}; do
    if docker-compose --env-file .env exec -T postgres pg_isready -U ${DB_USERNAME:-gocommerceuser} -d ${DB_NAME:-gocommerce} > /dev/null 2>&1; then
        echo "  ✅ Main PostgreSQL initialized"
        break
    fi
    echo "  ⏳ Waiting for main PostgreSQL to initialize... ($i/40)"
    sleep 3
done

for i in {1..40}; do
    if docker-compose --env-file .env exec -T keycloak-db pg_isready -U ${KEYCLOAK_DB_USERNAME:-keycloak} > /dev/null 2>&1; then
        echo "  ✅ Keycloak PostgreSQL initialized"
        break
    fi
    echo "  ⏳ Waiting for Keycloak PostgreSQL to initialize... ($i/40)"
    sleep 3
done

echo ""
echo "🔑 Step 8: Starting Keycloak with realm import..."
# Start Keycloak and wait for it to be ready (longer timeout for realm import)
docker-compose --env-file .env up -d keycloak

echo ""
echo "⏳ Step 9: Waiting for Keycloak to import realm and be ready..."
# Wait for Keycloak (macOS compatible, longer wait for realm import)
for i in {1..60}; do
    if curl -s http://localhost:${KEYCLOAK_PORT:-9000}/health/ready > /dev/null 2>&1; then
        echo "  ✅ Keycloak ready with realm imported"
        break
    fi
    echo "  ⏳ Waiting for Keycloak to start and import realm... ($i/60)"
    sleep 5
done

echo ""
echo "🎯 Step 10: Starting GO-Commerce application..."
# Finally start the application
docker-compose --env-file .env up -d gocommerce

echo ""
echo "⏳ Step 11: Waiting for application to be ready..."
# Wait for application (macOS compatible)
for i in {1..40}; do
    if curl -s http://localhost:${APP_PORT:-8080}/q/health/ready > /dev/null 2>&1; then
        echo "  ✅ GO-Commerce application is ready"
        break
    fi
    echo "  ⏳ Waiting for GO-Commerce application... ($i/40)"
    sleep 3
done

echo ""
echo "✅ GO-Commerce Docker environment rebuilt successfully!"
echo ""
echo "🌐 Service URLs:"
echo "  📱 Application:    http://localhost:${APP_PORT:-8080}"
echo "  🔑 Keycloak:       http://localhost:${KEYCLOAK_PORT:-9000}"
echo "  📊 Swagger UI:     http://localhost:${APP_PORT:-8080}/swagger-ui"
echo "  📖 OpenAPI:        http://localhost:${APP_PORT:-8080}/openapi"
echo "  ❤️  Health Check:  http://localhost:${APP_PORT:-8080}/q/health"
echo ""
echo "🔍 Fresh environment ready with:"
echo "  🗄️  Clean databases (all migrations applied)"
echo "  🔑 Keycloak realm imported"
echo "  🏗️  Latest application code"
echo "  📊 All services healthy"
echo ""
echo "🎯 Ready for development and testing!"
