package dev.tiodati.saas.gocommerce.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dev.tiodati.saas.gocommerce.model.tenant.Tenant;
import dev.tiodati.saas.gocommerce.model.tenant.TenantStatus;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
public class TenantSchemaResolverTest {

    @Inject
    @PersistenceUnitExtension("gocommerce")
    TenantSchemaResolver tenantSchemaResolver;
    
    @InjectMock
    CurrentVertxRequest currentVertxRequest;
    
    @InjectMock
    EntityManager entityManager;
    
    private RoutingContext routingContext;
    private HttpServerRequest httpRequest;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        // Set up mock objects
        routingContext = Mockito.mock(RoutingContext.class);
        httpRequest = Mockito.mock(HttpServerRequest.class);
        
        // Set up test tenant
        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setTenantKey("test-tenant");
        testTenant.setName("Test Tenant");
        testTenant.setSubdomain("test");
        testTenant.setStatus(TenantStatus.ACTIVE);
        testTenant.setSchemaName("tenant_test-tenant");
        
        // Set up routing context
        when(routingContext.request()).thenReturn(httpRequest);
        when(currentVertxRequest.getCurrent()).thenReturn(routingContext);
    }
    
    @Test
    @Order(1)
    void testGetDefaultTenantId() {
        assertEquals("default", tenantSchemaResolver.getDefaultTenantId());
    }
    
    @Test
    @Order(4)
    void testResolveTenantId_ignoreWwwSubdomain() {
        // Arrange
        when(httpRequest.getHeader("X-Tenant")).thenReturn(null);
        when(httpRequest.getHeader("Host")).thenReturn("www.example.com");
        
        // Act
        String result = tenantSchemaResolver.resolveTenantId();
        
        // Assert
        assertEquals("default", result);
    }
    
    @Test
    @Order(6)
    void testResolveTenantId_fallbackToDefault_whenNoVertxRequest() {
        // Arrange
        when(currentVertxRequest.getCurrent()).thenReturn(null);
        
        // Act
        String result = tenantSchemaResolver.resolveTenantId();
        
        // Assert
        assertEquals("default", result);
    }
    
    @Test
    @Order(7)
    void testResolveTenantId_fallbackToDefault_whenNoContext() {
        // Arrange
        when(currentVertxRequest.getCurrent()).thenReturn(null);
        
        // Act
        String result = tenantSchemaResolver.resolveTenantId();
        
        // Assert
        assertEquals("default", result);
    }
}