package dev.tiodati.saas.gocommerce.testinfra;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation for tests that require multi-tenant database setup.
 * 
 * This annotation automatically configures the test environment to:
 * - Create store-specific database schemas
 * - Set up tenant context for Hibernate
 * - Handle test data isolation
 * 
 * Usage:
 * <pre>
 * {@code
 * @QuarkusTest
 * @MultiTenantTest
 * class MyStoreTest {
 *     // Test methods that need store schema access
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MultiTenantTestExtension.class)
public @interface MultiTenantTest {
    
    /**
     * Optional custom schema name for the test.
     * If not specified, a unique schema will be generated per test class.
     */
    String schema() default "";
    
    /**
     * Whether to clean the schema after each test method.
     * Default is true for better test isolation.
     */
    boolean cleanAfterEach() default true;
    
    /**
     * Whether to drop the schema after all tests in the class complete.
     * Default is false to preserve schemas for debugging.
     */
    boolean dropAfterAll() default false;
}
