package dev.tiodati.saas.gocommerce.tenant;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

@QuarkusTest
public class RequestTenantFilterTest {
    
    private RequestTenantFilter filter;
    private ContainerRequestContext requestContext;
    private ContainerResponseContext responseContext;
    
    @BeforeEach
    void setUp() {
        filter = new RequestTenantFilter();
        requestContext = Mockito.mock(ContainerRequestContext.class);
        responseContext = Mockito.mock(ContainerResponseContext.class);
    }
    
    @Test
    void testFilter_clearsTenantContext() {
        // Arrange
        TenantContext.setCurrentTenant("test_tenant");
        
        // Act
        filter.filter(requestContext, responseContext);
        
        // Assert
        assertNull(TenantContext.getCurrentTenant());
    }
}