#!/bin/bash

# GO-Commerce Docker Test Runner
# Runs tests with required Docker dependencies (PostgreSQL, Keycloak)

set -e  # Exit on any error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Default test type
TEST_TYPE="${1:-standard}"

echo "🧪 GO-Commerce - Running tests with Docker dependencies..."
echo "📂 Project root: $PROJECT_ROOT"
echo "📂 Docker directory: $SCRIPT_DIR"
echo "🎯 Test type: $TEST_TYPE"

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
echo "🐘 Starting test dependencies (PostgreSQL + Keycloak)..."

# Start only the dependencies needed for testing
docker-compose --env-file .env up -d postgres keycloak-db

echo "⏳ Waiting for PostgreSQL to be ready..."
# Wait for PostgreSQL (macOS compatible)
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

echo "🔑 Starting Keycloak for integration tests..."
docker-compose --env-file .env up -d keycloak

echo "⏳ Waiting for Keycloak to be ready..."
# Wait for Keycloak (macOS compatible)
for i in {1..40}; do
    if curl -s http://localhost:${KEYCLOAK_PORT:-9000}/health/ready > /dev/null 2>&1; then
        echo "  ✅ Keycloak is ready"
        break
    fi
    echo "  ⏳ Waiting for Keycloak... ($i/40)"
    sleep 3
done

echo ""
echo "✅ Test dependencies are ready!"
echo ""

# Change to project root for Maven execution
cd "$PROJECT_ROOT"

# Function to run tests with proper cleanup
run_tests() {
    local test_command="$1"
    local test_description="$2"
    
    echo "🚀 Running $test_description..."
    echo "📝 Command: $test_command"
    echo ""
    
    # Set environment variables for tests to connect to Docker services
    export QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://localhost:${DB_PORT:-5433}/${DB_NAME:-gocommerce}"
    export QUARKUS_DATASOURCE_USERNAME="${DB_USERNAME:-gocommerceuser}"
    export QUARKUS_DATASOURCE_PASSWORD="${DB_PASSWORD:-gocommercepass}"
    export QUARKUS_OIDC_AUTH_SERVER_URL="http://localhost:${KEYCLOAK_PORT:-9000}/realms/gocommerce"
    export QUARKUS_OIDC_CLIENT_ID="${OIDC_CLIENT_ID:-gocommerce-client}"
    export QUARKUS_OIDC_CREDENTIALS_SECRET="${OIDC_CLIENT_SECRET:-NwASnqRj8AnokmBaDIVy9WTijHYrWHAe}"
    
    # Run the test command
    if eval "$test_command"; then
        echo ""
        echo "✅ $test_description completed successfully!"
        return 0
    else
        echo ""
        echo "❌ $test_description failed!"
        return 1
    fi
}

# Determine which tests to run based on the argument
case "$TEST_TYPE" in
    "standard"|"")
        run_tests "mvn test" "Standard Tests (Unit + Integration)"
        TEST_EXIT_CODE=$?
        ;;
    "integration")
        run_tests "mvn test -Dtest='**/*IT,**/*IntegrationTest'" "Integration Tests Only"
        TEST_EXIT_CODE=$?
        ;;
    "unit")
        run_tests "mvn test -Dtest='**/*Test' -Dtest.excludes='**/*IT,**/*IntegrationTest'" "Unit Tests Only"
        TEST_EXIT_CODE=$?
        ;;
    "all")
        echo "🎯 Running comprehensive test suite..."
        
        # Run checkstyle first
        run_tests "mvn checkstyle:check" "Code Style Check"
        CHECKSTYLE_EXIT_CODE=$?
        
        # Run all tests
        run_tests "mvn test" "All Tests"
        TEST_EXIT_CODE=$?
        
        # Calculate overall result
        TEST_EXIT_CODE=$((CHECKSTYLE_EXIT_CODE + TEST_EXIT_CODE))
        ;;
    "coverage")
        run_tests "mvn test jacoco:report" "Tests with Coverage Report"
        TEST_EXIT_CODE=$?
        echo ""
        echo "📊 Coverage report available at: target/site/jacoco/index.html"
        ;;
    *)
        echo "❌ Unknown test type: $TEST_TYPE"
        echo ""
        echo "📖 Available test types:"
        echo "  standard     - Run all standard tests (default)"
        echo "  integration  - Run integration tests only"  
        echo "  unit         - Run unit tests only"
        echo "  all          - Run checkstyle + all tests"
        echo "  coverage     - Run tests with coverage report"
        echo ""
        echo "📝 Usage: $0 [test-type]"
        TEST_EXIT_CODE=1
        ;;
esac

echo ""
echo "🧹 Cleaning up test dependencies..."

# Return to docker directory for cleanup
cd "$SCRIPT_DIR"

# Stop test dependencies (but keep data for next run)
docker-compose --env-file .env stop keycloak postgres keycloak-db

echo ""
if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo "🎉 Test run completed successfully!"
    echo ""
    echo "🔍 Next steps:"
    echo "  📊 Check test results in target/surefire-reports/"
    echo "  📱 Start full environment: ./run-docker.sh"
    echo "  🔄 Rebuild if needed: ./rebuild-docker.sh"
else
    echo "💥 Test run failed! Exit code: $TEST_EXIT_CODE"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "  📋 Check test logs in target/surefire-reports/"
    echo "  🐳 Check Docker logs: docker-compose --env-file .env logs"
    echo "  🔄 Try rebuilding: ./rebuild-docker.sh"
fi

echo ""

exit $TEST_EXIT_CODE
