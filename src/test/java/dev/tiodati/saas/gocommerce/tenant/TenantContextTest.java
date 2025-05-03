package dev.tiodati.saas.gocommerce.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TenantContextTest {

    @AfterEach
    void cleanup() {
        // Ensure TenantContext is cleared after each test
        TenantContext.clear();
    }

    @Test
    void testSetAndGetCurrentTenant() {
        // Arrange
        String tenantId = "test_tenant";
        
        // Act
        TenantContext.setCurrentTenant(tenantId);
        String result = TenantContext.getCurrentTenant();
        
        // Assert
        assertEquals(tenantId, result);
    }
    
    @Test
    void testClearTenant() {
        // Arrange
        TenantContext.setCurrentTenant("test_tenant");
        
        // Act
        TenantContext.clear();
        String result = TenantContext.getCurrentTenant();
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testMultipleThreadsTenantIsolation() throws InterruptedException {
        // Arrange
        final String tenantId1 = "tenant1";
        final String tenantId2 = "tenant2";
        final String[] threadResult1 = new String[1];
        final String[] threadResult2 = new String[1];
        
        // Act
        TenantContext.setCurrentTenant(tenantId1);
        
        Thread thread = new Thread(() -> {
            // This should be null since ThreadLocal is specific to each thread
            threadResult1[0] = TenantContext.getCurrentTenant();
            
            // Set tenant in this thread
            TenantContext.setCurrentTenant(tenantId2);
            threadResult2[0] = TenantContext.getCurrentTenant();
        });
        
        thread.start();
        thread.join(); // Wait for thread to complete
        
        // Assert
        assertNull(threadResult1[0]); // Should be null in the new thread initially
        assertEquals(tenantId2, threadResult2[0]); // Should be set in the new thread
        assertEquals(tenantId1, TenantContext.getCurrentTenant()); // Main thread should still have its value
    }
}