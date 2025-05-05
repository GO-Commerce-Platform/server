# Docker Environment Setup for GO-Commerce

This document explains how to set up and use the Docker environment for local development of the GO-Commerce platform.

## Prerequisites

- Docker and Docker Compose installed on your development machine
- Git repository cloned to your local machine

## Environment Configuration

The Docker environment uses a `.env` file for configuration. A default file is provided in the repository, but you should modify it according to your needs:

```
# GoCommerce - Docker Environment Variables

# MariaDB Configuration
DB_ROOT_PASSWORD=rootpassword  # Change this for production!
DB_NAME=gocommerce
DB_USERNAME=gocommerceuser
DB_PASSWORD=gocommercepass      # Change this for production!
DB_PORT=3306

# Keycloak Configuration
KEYCLOAK_DB_SCHEMA=keycloak
KEYCLOAK_DB_USERNAME=keycloak
KEYCLOAK_DB_PASSWORD=keycloak   # Change this for production!
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin   # Change this for production!
KEYCLOAK_PORT=9000

# Application Configuration
APP_PORT=8080
OIDC_CLIENT_ID=gocommerce-client
OIDC_CLIENT_SECRET=NwASnqRj8AnokmBaDIVy9WTijHYrWHAe  # Change this for production!

# Networking
HOST_DOMAIN=localhost
```

> **Note:** For production environments, make sure to change all passwords to strong, unique values.

## Services Overview

The Docker environment includes the following services:

1. **mariadb**: MariaDB database for application data
   - Port: 3306 (configurable in .env)
   - Credentials: Defined in .env file
   - Data persistence: Via volume mount to ./storage/mariadb_data

2. **keycloak**: Keycloak server for authentication and authorization
   - Port: 9000 (configurable in .env)
   - Admin credentials: Defined in .env file
   - Realm configuration: Imported from ./docker/keycloak-config

3. **keycloak-db**: PostgreSQL database for Keycloak
   - Data persistence: Via volume mount to ./storage/keycloak-db

4. **app**: Quarkus application container
   - Port: 8080 (configurable in .env)
   - Connects to MariaDB and Keycloak services

All services are connected via a Docker network called `gocommerce-network`.

## Starting the Environment

To start the Docker environment:

```bash
docker compose up -d
```

This will start all services in detached mode. To view logs:

```bash
docker compose logs -f
```

To view logs for a specific service (e.g., app):

```bash
docker compose logs -f app
```

## Development Workflow

1. **Building the application**:

   ```bash
   ./mvnw package
   ```

   For native compilation:

   ```bash
   ./mvnw package -Pnative
   ```

2. **Starting the application in dev mode**:

   ```bash
   ./mvnw quarkus:dev
   ```

   This runs outside Docker but can connect to the Docker services.

3. **Running with Docker**:

   ```bash
   docker compose up -d --build app
   ```

   This will rebuild and restart the app container.

## Accessing Services

- **Application**: http://localhost:8080
- **Keycloak Admin Console**: http://localhost:9000/admin
- **MariaDB**: localhost:3306 (use a database client)

## Stopping the Environment

To stop all services:

```bash
docker compose down
```

To stop all services and remove volumes (this will delete all data):

```bash
docker compose down -v
```

## Troubleshooting

1. **Database connection issues**:
   - Ensure the MariaDB container is healthy: `docker compose ps`
   - Check MariaDB logs: `docker compose logs mariadb`
   - Verify environment variables in .env match the application.properties

2. **Keycloak issues**:
   - Check Keycloak logs: `docker compose logs keycloak`
   - Ensure the Keycloak database is running: `docker compose ps keycloak-db`
   - Verify the realm import file is correctly formatted

3. **Network connectivity**:
   - All services are on the `gocommerce-network` Docker network
   - Services can reach each other using their service name as hostname
   - Example: The app connects to MariaDB using `mariadb` as the hostname