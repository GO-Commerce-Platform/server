# Contributing to GO-Commerce

Thank you for your interest in contributing to the GO-Commerce project! This document provides guidelines and standards for contributing to the codebase.

## Core Development Principles

### 1. Simplicity and Practicality First

**Avoid overengineering at all costs.** This is our most important principle. We value simple, maintainable solutions over complex architectures.

- Implement the minimum viable solution first
- Add complexity only when justified by actual requirements
- Don't create abstractions for hypothetical future needs
- Prefer standard framework features over custom implementations
- Choose readability and maintainability over cleverness
- Use design patterns appropriately, not dogmatically

### 2. Code Quality Standards

- Follow SOLID principles, but don't overdo it
- Write unit tests for business logic (aim for >80% coverage)
- Keep methods small and focused (≤20 lines preferred)
- Use meaningful names that clearly express intent
- Document public APIs and non-obvious implementations
- Follow consistent code style

### 3. Testing Requirements

- Unit tests for all service methods
- Integration tests for all endpoints
- Test both success and error cases
- Mock external dependencies
- Use TestSecurity annotation for testing secured endpoints
- Keep test code clean and maintainable

## Development Workflow

### Before You Start

1. **Understand the issue:** Read the requirements thoroughly
2. **Check the docs:** Review relevant documentation in the `/wiki` directory
3. **Plan your approach:** Consider the simplest solution that meets requirements
4. **Ask questions:** If something is unclear, ask before implementing

### Creating a Branch

Create branches from `main` using the naming format `username/issueXX`:

```bash
git checkout main
git pull
git checkout -b aquele-dinho/issue25
```

### Commits

Write clear, descriptive commit messages:

```
Issue #25: Add order validation service

- Create OrderValidationService with inventory check 
- Add unit tests for validation logic
- Update order placement flow to use validation
```

### Testing Your Changes

Before submitting:

```bash
mvn clean test
```

## Code Style Guidelines

### Package Structure: Package by Feature

The project adheres to a **Package by Feature** organizational strategy. This means that source code is structured around business functionalities or domains rather than technical layers. Each primary feature should reside in its own top-level package under `dev.tiodati.saas.gocommerce`.

For example:
- `dev.tiodati.saas.gocommerce.platform` (for platform administration)
- `dev.tiodati.saas.gocommerce.store` (for individual store operations)
- `dev.tiodati.saas.gocommerce.product` (for product management)

Within each feature package, you can further organize by technical role if needed (e.g., `dto/`, `resource/`, `service/`, `model/`).

**Guiding Principles:**
- **High Cohesion:** Keep code related to a single feature together.
- **Low Coupling:** Minimize dependencies between different feature packages.
- **Clarity:** Make it easy to locate code related to a specific feature.
- **Shared Code:** Place genuinely cross-cutting concerns (e.g., generic utilities, base classes) in a `dev.tiodati.saas.gocommerce.shared` package.

This approach helps in maintaining a modular, scalable, and understandable codebase, especially as the project grows.

### Simple > Complex

Always favor simpler solutions:

```java
// GOOD - Simple, readable approach
public List<Product> getActiveProducts() {
    return productRepository.findByStatus("ACTIVE");
}

// AVOID - Overengineered solution
public List<Product> getActiveProducts() {
    return productRepository.findAll().stream()
        .filter(product -> ProductStatus.ACTIVE.equals(product.getStatus()))
        .collect(Collectors.toList());
}
```

### Avoid Overabstraction

Don't create unnecessary abstractions:

```java
// GOOD - Direct, clear implementation
@Service
public class ProductService {
    private final ProductRepository repository;
    
    // Implementation...
}

// AVOID - Unnecessary abstraction for simple service
public interface ProductService {
    List<Product> findAll();
}

@Service
public class ProductServiceImpl implements ProductService {
    // Implementation...
}
```

### Use Framework Features

Leverage the framework instead of reinventing solutions:

```java
// GOOD - Using Quarkus/Jakarta validation
public record ProductRequest(
    @NotBlank String name,
    @NotNull @Positive BigDecimal price
) {}

// AVOID - Custom validation
public class ProductRequest {
    private String name;
    private BigDecimal price;
    
    public void validate() {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Name is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Price must be positive");
        }
    }
}
```

### Testing Should Be Simple Too

Keep tests focused and readable:

```java
// GOOD - Clear, focused test
@Test
void shouldReturnActiveProducts() {
    // Given
    given(repository.findByStatus("ACTIVE")).willReturn(List.of(product1, product2));
    
    // When
    List<Product> result = service.getActiveProducts();
    
    // Then
    assertEquals(2, result.size());
}

// AVOID - Overengineered test setup
@Test
void shouldReturnActiveProducts() {
    // Complex test setup with unnecessary abstractions and helpers
    TestDataBuilder builder = new TestDataBuilder();
    ProductTestFixture fixture = builder.withProducts(3)
        .withStatus("ACTIVE")
        .withPriceRange(10.0, 20.0)
        .build();
        
    // Test execution and verification
}
```

## Pull Request Process

1. Ensure all tests pass and code meets quality standards
2. Update documentation if necessary
3. Submit a PR with a clear title referencing the issue
4. Respond to code review feedback

## Questions?

If you have any questions about contributing, please reach out to the team through the project's communication channels.