# GO-Commerce

A multi-tenant e-commerce SaaS platform built with Quarkus and event-driven architecture.

## Project Overview

GO-Commerce is a scalable, multi-tenant e-commerce platform designed to support multiple storefronts with complete data isolation. The platform uses an event-driven architecture with Apache Kafka for asynchronous communication between services.

## Technology Stack

- **Backend Framework**: Quarkus
- **Database**: MariaDB (multi-tenant data), PostgreSQL (Keycloak)
- **Authentication**: Keycloak for identity and access management
- **Messaging**: Apache Kafka for event-driven architecture
- **Containerization**: Docker & Docker Compose

## Development Phases

The project is being developed in phases, starting with an MVP:

### Phase 1: MVP
- Multi-tenant foundation with schema-per-tenant approach
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

2. Start the containers
   ```
   cd gocommerce
   docker-compose up -d
   ```

3. Run the application in dev mode
   ```
   ./mvnw quarkus:dev
   ```

## License

This project is licensed under the Apache License 2.0
