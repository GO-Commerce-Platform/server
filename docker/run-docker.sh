#!/bin/bash

# GO-Commerce Docker Run Script
# Builds and starts all required containers for the complete application stack

set -e  # Exit on any error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "🚀 GO-Commerce - Starting complete Docker environment..."
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

echo "🔨 Building application Docker image..."
# Build the application first to ensure latest code is included
docker-compose --env-file .env build gocommerce

echo "🐘 Starting PostgreSQL databases..."
# Start databases first and wait for them to be healthy
docker-compose --env-file .env up -d postgres keycloak-db

echo "⏳ Waiting for databases to be ready..."
# Wait for PostgreSQL to be ready
# Wait for PostgreSQL to be ready (macOS compatible)
for i in {1..30}; do
    if docker-compose --env-file .env exec -T postgres pg_isready -U ${DB_USERNAME:-gocommerceuser} -d ${DB_NAME:-gocommerce} > /dev/null 2>&1; then
        echo "  ✅ Main PostgreSQL is ready"
        break
    fi
    echo "  ⏳ Waiting for main PostgreSQL... ($i/30)"
    sleep 2
done

for i in {1..30}; do
    if docker-compose --env-file .env exec -T keycloak-db pg_isready -U ${KEYCLOAK_DB_USERNAME:-keycloak} > /dev/null 2>&1; then
        echo "  ✅ Keycloak PostgreSQL is ready"
        break
    fi
    echo "  ⏳ Waiting for Keycloak PostgreSQL... ($i/30)"
    sleep 2
done

echo "🔑 Starting Keycloak..."
# Start Keycloak and wait for it to be ready
docker-compose --env-file .env up -d keycloak

echo "⏳ Waiting for Keycloak to be ready..."
# Wait for Keycloak to be ready (macOS compatible)
for i in {1..40}; do
    if curl -s http://localhost:${KEYCLOAK_PORT:-9000}/health/ready > /dev/null 2>&1; then
        echo "  ✅ Keycloak is ready"
        break
    fi
    echo "  ⏳ Waiting for Keycloak to start... ($i/40)"
    sleep 3
done

echo "🎯 Starting GO-Commerce application..."
# Finally start the application
docker-compose --env-file .env up -d gocommerce

echo ""
echo "✅ GO-Commerce Docker environment started successfully!"
echo ""
echo "🌐 Service URLs:"
echo "  📱 Application:    http://localhost:${APP_PORT:-8080}"
echo "  🔑 Keycloak:       http://localhost:${KEYCLOAK_PORT:-9000}"
echo "  📊 Swagger UI:     http://localhost:${APP_PORT:-8080}/swagger-ui"
echo "  📖 OpenAPI:        http://localhost:${APP_PORT:-8080}/openapi"
echo ""
echo "🔍 Useful commands:"
echo "  📋 View logs:      docker-compose --env-file .env logs -f [service]"
echo "  📊 Check status:   docker-compose --env-file .env ps"
echo "  🛑 Stop all:       docker-compose --env-file .env down"
echo ""
echo "🎯 Ready for development and testing!"
