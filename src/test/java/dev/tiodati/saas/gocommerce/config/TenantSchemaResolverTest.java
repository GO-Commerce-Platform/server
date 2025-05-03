package dev.tiodati.saas.gocommerce.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.tenant.TenantContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

@QuarkusTest
public class TenantSchemaResolverTest {

    // Create a test subclass of TenantSchemaResolver we can control
    static class TestTenantSchemaResolver extends TenantSchemaResolver {
        private String schemaNameForSubdomain;
        private String schemaNameForTenantKey;
        
        public void setSchemaNameForSubdomain(String schemaName) {
            this.schemaNameForSubdomain = schemaName;
        }
        
        public void setSchemaNameForTenantKey(String schemaName) {
            this.schemaNameForTenantKey = schemaName;
        }
        
        @Override
        protected String getSchemaNameFromSubdomain(String subdomain) {
            return schemaNameForSubdomain;
        }
        
        @Override
        protected String getSchemaNameFromTenantKey(String tenantKey) {
            return schemaNameForTenantKey;
        }
    }
    
    private TestTenantSchemaResolver testResolver;
    
    @InjectMock
    CurrentVertxRequest currentVertxRequest;
    
    @InjectMock
    EntityManager entityManager;
    
    private RoutingContext routingContext;
    private HttpServerRequest httpRequest;

    @BeforeEach
    void setUp() {
        // Set up mock objects
        routingContext = mock(RoutingContext.class);
        httpRequest = mock(HttpServerRequest.class);
        
        // Set up routing context
        when(routingContext.request()).thenReturn(httpRequest);
        when(currentVertxRequest.getCurrent()).thenReturn(routingContext);
        
        // Create our test resolver
        testResolver = new TestTenantSchemaResolver();
        testResolver.currentVertxRequest = currentVertxRequest;
        testResolver.em = entityManager;
        
        // Clear tenant context before each test
        TenantContext.clear();
    }
    
    @Test
    @Order(1)
    void testGetDefaultTenantId() {
        assertEquals("default", testResolver.getDefaultTenantId());
    }
    
    @Test
    @Order(2)
    void testResolveTenantIdFromSubdomain() {
        // Arrange
        when(httpRequest.getHeader("X-Tenant")).thenReturn(null); // No tenant header
        when(httpRequest.getHeader("Host")).thenReturn("test.example.com");
        testResolver.setSchemaNameForSubdomain("tenant_test-tenant");
        
        // Act
        String result = testResolver.resolveTenantId();
        
        // Assert
        assertEquals("tenant_test-tenant", result);
    }
    
    @Test
    @Order(3) 
    void testResolveTenantIdFromHeader() {
        // Arrange
        when(httpRequest.getHeader("X-Tenant")).thenReturn("test-tenant");
        testResolver.setSchemaNameForTenantKey("tenant_test-tenant");
        
        // Act
        String result = testResolver.resolveTenantId();
        
        // Assert
        assertEquals("tenant_test-tenant", result);
    }
    
    @Test
    @Order(4)
    void testResolveTenantId_ignoreWwwSubdomain() {
        // Arrange
        when(httpRequest.getHeader("X-Tenant")).thenReturn(null);
        when(httpRequest.getHeader("Host")).thenReturn("www.example.com");
        
        // Act
        String result = testResolver.resolveTenantId();
        
        // Assert
        assertEquals("default", result);
    }
    
    @Test
    @Order(5)
    void testResolveTenantIdFromContext() {
        // Arrange
        TenantContext.setCurrentTenant("context_tenant");
        
        // Act
        String result = testResolver.resolveTenantId();
        
        // Assert
        assertEquals("context_tenant", result);
    }
    
    @Test
    @Order(6)
    void testResolveTenantId_fallbackToDefault_whenNoVertxRequest() {
        // Arrange
        when(currentVertxRequest.getCurrent()).thenReturn(null);
        
        // Act
        String result = testResolver.resolveTenantId();
        
        // Assert
        assertEquals("default", result);
    }
    
    @Test
    @Order(7)
    void testResolveTenantId_fallbackToDefault_whenNoContext() {
        // Arrange
        when(currentVertxRequest.getCurrent()).thenReturn(null);
        
        // Act
        String result = testResolver.resolveTenantId();
        
        // Assert
        assertEquals("default", result);
    }
    
    @Test
    @Order(8)
    void testResolveTenantId_storesResolvedTenantInContext() {
        // Arrange
        when(httpRequest.getHeader("X-Tenant")).thenReturn("test-tenant");
        testResolver.setSchemaNameForTenantKey("tenant_test-tenant");
        
        // Act
        String result = testResolver.resolveTenantId();
        
        // Assert
        assertEquals("tenant_test-tenant", result);
        assertEquals("tenant_test-tenant", TenantContext.getCurrentTenant());
    }
}