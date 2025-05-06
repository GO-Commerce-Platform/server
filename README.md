# GO-Commerce

A multi-store e-commerce SaaS platform built with Quarkus and event-driven architecture.

## Project Overview

GO-Commerce is a scalable, multi-store e-commerce platform designed to support multiple storefronts with complete data isolation. The platform uses an event-driven architecture with Apache Kafka for asynchronous communication between services.

## Technology Stack

- **Backend Framework**: Quarkus
- **Database**: MariaDB (multi-store data), PostgreSQL (Keycloak)
- **Authentication**: Keycloak for identity and access management
- **Messaging**: Apache Kafka for event-driven architecture
- **Containerization**: Docker & Docker Compose

## Development Phases

The project is being developed in phases, starting with an MVP:

### Phase 1: MVP
- Multi-store foundation with schema-per-store approach
- Core authentication with Keycloak integration
- Basic product and inventory management
- Essential customer profiles
- Simple order processing
- Standard pricing model

### Phase 2: Advanced Features
- Product kits and combos
- Advanced inventory management
- Flexible pricing and discount rules
- Enhanced order workflows

### Phase 3: Integration & Scale
- Complete event-driven architecture
- External system integrations
- Performance optimizations
- Advanced multi-tenancy features

## Documentation

The complete documentation for this project is available in the [Wiki](https://github.com/aquele-dinho/GO-Commerce/wiki):

1. [Document Structure](https://github.com/aquele-dinho/GO-Commerce/wiki/00-Document-Structure)
2. [Project Charter](https://github.com/aquele-dinho/GO-Commerce/wiki/01-Project-Charter)
3. [Roadmap](https://github.com/aquele-dinho/GO-Commerce/wiki/02-Roadmap)
4. [User Stories](https://github.com/aquele-dinho/GO-Commerce/wiki/03-User-Story)
5. [Technical Design Document](https://github.com/aquele-dinho/GO-Commerce/wiki/04-Technical-Design-Document)
6. [Technical Solution Specification](https://github.com/aquele-dinho/GO-Commerce/wiki/05-Technical-Solution-Specification)
7. [Data Model](https://github.com/aquele-dinho/GO-Commerce/wiki/06-Data-Model)
8. [Test Plan](https://github.com/aquele-dinho/GO-Commerce/wiki/07-Test-Plan)
9. [MVP Planning](https://github.com/aquele-dinho/GO-Commerce/wiki/08-MVP-Planning)

## Getting Started

1. Clone the repository
   ```
   git clone https://github.com/aquele-dinho/GO-Commerce.git
   ```

2. Set up environment variables
   - Review and update the `.env` file in the `docker` directory with your desired configuration settings

3. Start the application with Docker
   ```
   cd gocommerce
   ./docker/run-docker.sh
   ```
   This script builds the application and starts all required containers:
   - MariaDB (database)
   - Keycloak and PostgreSQL (authentication)
   - The Quarkus application

4. Alternatively, run just the infrastructure in Docker and the application in dev mode
   ```
   # Start supporting services (database, Keycloak)
   cd gocommerce
   docker-compose --env-file ./docker/.env up -d

   # Run the application in dev mode in a separate terminal
   mvn quarkus:dev
   ```

5. To completely rebuild your environment (useful after pulling updates)
   ```
   cd gocommerce
   ./docker/rebuild-docker.sh
   ```

6. To run tests with Docker dependencies
   ```
   cd gocommerce
   ./docker/run-tests.sh        # Run standard tests
   ./docker/run-tests.sh all    # Run all tests
   ./docker/run-tests.sh integration  # Run integration tests
   ```

## Docker Structure

The Docker configuration is organized as follows:
- `/docker` - Contains all Docker-related files
  - `.env` - Environment variables for all services
  - `docker-compose.yml` - Defines all services (database, Keycloak, application)
  - `app/` - Contains Dockerfiles for different deployment scenarios
    - `Dockerfile.jvm` - For running the application in JVM mode
    - `Dockerfile.native` - For running the application as a native executable
    - `Dockerfile.legacy-jar` - For running with legacy JAR packaging
    - `Dockerfile.native-micro` - For minimal native executable containers
  - `keycloak-config/` - Contains Keycloak realm configuration
  - `run-docker.sh` - Helper script to build and run the application
  - `rebuild-docker.sh` - Script to tear down and rebuild the entire environment
  - `run-tests.sh` - Script to run tests with required Docker dependencies

## License

This project is dual-licensed:

- **For personal, educational, and non-commercial use**: GNU Affero General Public License v3.0 (AGPL-3.0)
- **For commercial use**: A separate commercial license is required. Please see the [COMMERCIAL_LICENSE](./COMMERCIAL_LICENSE) file for details or contact us at contato@TioDaTI.dev

For more details, please see the [LICENSE](./LICENSE) file.
