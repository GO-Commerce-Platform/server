# Project Context

## Core Business Logic

GO-Commerce is a multi-tenant e-commerce platform designed to enable tenants (stores) to manage their own products, orders, and customers. The platform supports schema-per-store multi-tenancy, allowing each store to operate independently while sharing the same application infrastructure. Key features include product management, shopping cart functionality, customer management, and integration with Keycloak for authentication and authorization.

## Key Data Models

-   **Store**: Represents a tenant in the multi-tenant architecture, including store-specific configurations.
-   **StoreAdmin**: Manages administrative tasks for a store.
-   **Product**: Represents items available for purchase within a store.
-   **Category**: Organizes products into hierarchical groups.
-   **Customer**: Represents end-users who interact with stores.
-   **ShoppingCart**: Tracks items selected by customers for purchase.
-   **CartItem**: Represents individual items within a shopping cart.
-   **Roles**: Defines user roles and permissions for access control.

## Inferred Architecture

GO-Commerce is built as a monolithic application using the Quarkus framework for its backend. It employs a schema-per-tenant multi-store model for database management, leveraging MariaDB as the primary database. Authentication and authorization are handled via Keycloak, integrated with the platform. The architecture is event-driven, ensuring scalability and responsiveness, and follows modern Java practices with a feature-based package structure.
