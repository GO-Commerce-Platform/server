package dev.tiodati.saas.gocommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dev.tiodati.saas.gocommerce.model.tenant.Tenant;
import dev.tiodati.saas.gocommerce.model.tenant.TenantAdmin;
import dev.tiodati.saas.gocommerce.model.tenant.TenantStatus;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery; // Ensure TypedQuery is imported

@QuarkusTest
public class TenantServiceTest {

    @Inject
    TenantService tenantService;

    @InjectMock
    EntityManager entityManager;

    private Tenant testTenant;
    private TenantAdmin testAdmin;
    // Use TypedQuery<Tenant> for the mock object
    private TypedQuery<Tenant> mockTypedQuery;

    @BeforeEach
    void setUp() {
        // Setup test data
        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setTenantKey("test-tenant");
        testTenant.setName("Test Tenant");
        testTenant.setSubdomain("test");
        testTenant.setStatus(TenantStatus.ACTIVE);
        testTenant.setBillingPlan("BASIC");
        testTenant.setSchemaName("tenant_test-tenant");

        testAdmin = new TenantAdmin();
        testAdmin.setEmail("admin@test.com");
        testAdmin.setPasswordHash("hashedpassword");
        testAdmin.setFirstName("Test");
        testAdmin.setLastName("Admin");

        // Mock TypedQuery<Tenant> specifically
        mockTypedQuery = Mockito.mock(TypedQuery.class);
    }

    @Test
    void testFindById_whenTenantExists_shouldReturnTenant() {
        // Arrange
        when(entityManager.find(Tenant.class, 1L)).thenReturn(testTenant);

        // Act
        Optional<Tenant> result = tenantService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test-tenant", result.get().getTenantKey());
        assertEquals("Test Tenant", result.get().getName());
    }

    @Test
    void testFindById_whenTenantDoesNotExist_shouldReturnEmpty() {
        // Arrange
        when(entityManager.find(Tenant.class, 999L)).thenReturn(null);

        // Act
        Optional<Tenant> result = tenantService.findById(999L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testCreateTenant_shouldPersistTenantAndAdmin() {
        // Arrange
        Tenant newTenant = new Tenant();
        newTenant.setTenantKey("new-tenant");
        newTenant.setName("New Tenant");
        newTenant.setSubdomain("NewSubdomain");
        newTenant.setStatus(TenantStatus.TRIAL);
        newTenant.setBillingPlan("PREMIUM");

        TenantAdmin newAdmin = new TenantAdmin();
        newAdmin.setEmail("admin@newtenant.com");
        newAdmin.setPasswordHash("password123");
        newAdmin.setFirstName("New");
        newAdmin.setLastName("Admin");

        // Act
        tenantService.createTenant(newTenant, newAdmin);

        // Assert
        verify(entityManager).persist(newTenant);
        verify(entityManager).persist(newAdmin);
        assertEquals("newsubdomain", newTenant.getSubdomain()); // Should be normalized to lowercase
        assertEquals("tenant_new-tenant", newTenant.getSchemaName());
    }

    @Test
    void testCreateTenant_withExistingId_shouldThrowException() {
        // Arrange
        Tenant invalidTenant = new Tenant();
        invalidTenant.setId(5L); // Already has ID
        invalidTenant.setTenantKey("invalid");

        TenantAdmin admin = new TenantAdmin();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.createTenant(invalidTenant, admin));
        assertEquals("New tenant cannot have an ID", exception.getMessage());
    }

    @Test
    void testUpdateTenant_whenTenantDoesNotExist_shouldThrowException() {
        // Arrange
        Tenant nonExistentTenant = new Tenant();
        nonExistentTenant.setId(999L); // Non-existent ID
        nonExistentTenant.setName("Update Attempt");

        when(entityManager.find(Tenant.class, 999L)).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.updateTenant(nonExistentTenant));
        assertTrue(exception.getMessage().contains("Tenant not found"));
    }

    @Test
    void testUpdateTenantStatus_shouldChangeStatus() {
        // Arrange
        when(entityManager.find(Tenant.class, 1L)).thenReturn(testTenant);

        // Act
        tenantService.updateTenantStatus(1L, TenantStatus.INACTIVE);

        // Assert
        assertEquals(TenantStatus.INACTIVE, testTenant.getStatus());
    }

    @Test
    void testUpdateTenantStatus_withNonexistentTenant_shouldThrowException() {
        // Arrange
        when(entityManager.find(Tenant.class, 999L)).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.updateTenantStatus(999L, TenantStatus.INACTIVE));
        assertTrue(exception.getMessage().contains("Tenant not found"));
    }
}