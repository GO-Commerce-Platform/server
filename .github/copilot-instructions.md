# GitHub Copilot - Project Instructions

**Goal**: These instructions guide GitHub Copilot in generating, refactoring, and understanding Java code for the current project. The primary focus is on modern Java practices, verbosity reduction, code consistency, and adherence to project-specific conventions.

## Language
-   **Language**: Use English for chatting, code comments, documentation, and commit messages.

## Tone & Humor
-   **On Code & Comments**: Professional and concise.
-   **On Documentation**: Clear, informative, and helpful.
-   **On Commit Messages**: Use the imperative mood (e.g., "Fix bug", "Add feature") and be descriptive.
-   **On Chat**: Friendly and professional, with a touch of acid humor when appropriate.

## General Principles

-   **Adherence**: Follow these instructions carefully. They are designed to ensure code quality and consistency.
-   **Architecture Principles**: SOLID principles, DRY (Don't Repeat Yourself), and KISS (Keep It Simple, Stupid) should guide your design decisions.
-   **Architecture Awareness**: Understand the project's architecture, including the multi-store model (multi-tenant), event-driven design, and package structure.
-   **Feature Package Structure**: Code is organized by feature, not by layer. Each feature package contains its own resources, services, models, and repositories.
-   **Modern Java Practices**: Use Java 17+ features, including records, type inference, and functional programming paradigms.
-   **Verbosity Reduction**: Aim to reduce boilerplate code using records, Lombok, and fluent interfaces.
-   **Clarity**: If any instruction is unclear or seems to conflict with a specific task, please ask for clarification.
-   **Readability & Maintainability**: Prioritize creating code that is easy to read, understand, and maintain.
-   **Proactive Error Checking**: After generating or modifying code, anticipate potential issues. For Java files, consider using `get_errors` to validate changes.
-   **Tool Usage**:
    -   When refactoring, always read the relevant file content first.
    -   Prefer `semantic_search` for exploring code if unsure about exact names or locations.
    -   If asked to run or build the project, check for existing VS Code tasks (e.g., `quarkus:dev`) before resorting to raw terminal commands.

## Java Verbosity Reduction Guidelines

-   **Use Records for DTOs**:
    -   Implement all request/response Data Transfer Objects (DTOs) as Java records.
    -   Use records for any simple data-carrying classes.
    -   _Example_: `public record ProductDto(UUID id, String name, String description, BigDecimal price) {}`
-   **Apply Lombok Strategically**:
    -   **Preference Order**: Prefer Records > `@Value` (for immutable classes not suitable as records) > `@Data` (for mutable classes, but DTOs should be records).
    -   Use `@Builder` for complex objects that benefit from the builder pattern.
    -   Use `@RequiredArgsConstructor` with `final` fields for dependency injection, preferring constructor injection over field injection.
    -   _Example (Service)_: `@RequiredArgsConstructor @Slf4j public class ProductService { private final ProductRepository repository; }`
-   **Leverage Type Inference (`var`)**:
    -   Use `var` for local variables when the type is clearly obvious from the initializer.
    -   Avoid `var` if it reduces clarity or the type is not immediately apparent from the context.
    -   _Example_: `var products = productRepository.findByCategory(categoryId);`
-   **Implement Fluent Interfaces**:
    -   Design builders, configuration classes, and other suitable classes with fluent interfaces.
    -   Setter-like methods in fluent APIs should return `this` to enable method chaining.
    -   _Example_: `return new ProductBuilder().withName("Product").withPrice(BigDecimal.TEN).build();`
-   **Embrace Functional Programming**:
    -   Use lambdas and method references with the Streams API for collection processing.
    -   Implement functional interfaces for behavior parameterization where appropriate.
    -   Use `Optional` correctly and consistently to handle potentially absent values and avoid null checks.
    -   _Example_: `products.stream().filter(Product::isActive).map(ProductMapper::toDto).toList();`
-   **Avoid Boilerplate Patterns**:
    -   Do not create getters/setters manually; use records or Lombok annotations.
    -   Avoid manual implementation of the traditional Builder pattern; use Lombok's `@Builder`.
    -   Favor static factory methods (e.g., `Product.fromDto(productDto)`) over public constructors when they improve readability, provide better-named construction, or encapsulate complex creation logic.

## Key File & Directory Locations (Project Root Relative)

-   **This Document**: `.github/copilot-instructions.md`
-   **Main README**: `README.md` (Project overview, setup, entry point)
-   **Detailed Documentation (Wiki)**: `/wiki/` (Contains Project Charter, Roadmap, Technical Design, Data Model, etc.)
-   **Docker Configuration**:
    -   Main Application Compose: `/docker/docker-compose.yml`
    -   Application Dockerfiles: `/docker/app/`
    -   Environment Variables Template: `/docker/.env` (if present, or as per `docker-compose.yml` instructions)
    -   Root Compose File: `docker-compose.yml` (if used for broader development environment)
-   **Source Code (Java)**: `/src/main/java/` (Organized by feature)
-   **Resources**: `/src/main/resources/` (`application.properties`, `import.sql`, message bundles)
-   **Tests (Java)**: `/src/test/java/`
-   **Maven Project File**: `pom.xml`
-   **License Files**: `LICENSE`, `COMMERCIAL_LICENSE`
-   **Checkstyle Configuration**: `checkstyle.xml`, `checkstyle-excludes.xml`
-   **Scripts**: `/scripts/` (Utility and CI scripts)

## Project-Specific Conventions
- **Logging**:
    -   Use `io.quarkus.logging.Log` for logging.
    -   Avoid using `System.out.println` or other non-Quarkus logging methods.
- **Error Handling**:
    -   Use Quarkus's built-in exception handling mechanisms.
    -   Avoid using generic exceptions; prefer specific exceptions (e.g., `NotFoundException`, `BadRequestException`).
- **AI use disclaimer**:
    -  Add a comment at the end of eache file indicating it may have been generated by Copilot, e.g., `// Copilot: This file may have been generated or refactored by GitHub Copilot.`


