package dev.tiodati.saas.gocommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dev.tiodati.saas.gocommerce.model.tenant.Tenant;
import dev.tiodati.saas.gocommerce.model.tenant.TenantAdmin;
import dev.tiodati.saas.gocommerce.model.tenant.TenantStatus;
import dev.tiodati.saas.gocommerce.tenant.SchemaManager;
import dev.tiodati.saas.gocommerce.tenant.TenantContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

@QuarkusTest
@Transactional
public class TenantServiceTest {

    @Inject
    TenantService tenantService;

    @Inject
    EntityManager entityManager;
    
    @InjectMock
    SchemaManager schemaManager;
    
    @InjectMock
    TenantSettingsService settingsService;

    private Tenant testTenant;
    private TenantAdmin testAdmin;
    private TypedQuery<Tenant> mockTypedQuery;
    private UUID testTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        // Setup test data
        testTenant = new Tenant();
        testTenant.setId(testTenantId);
        testTenant.setTenantKey("test-tenant");
        testTenant.setName("Test Tenant");
        testTenant.setSubdomain("test");
        testTenant.setStatus(TenantStatus.ACTIVE);
        testTenant.setBillingPlan("BASIC");
        testTenant.setSchemaName("tenant_test-tenant");
        testTenant.setDeleted(false);
        testTenant.setVersion(1);

        testAdmin = new TenantAdmin();
        testAdmin.setEmail("admin@test.com");
        testAdmin.setPasswordHash("hashedpassword");
        testAdmin.setFirstName("Test");
        testAdmin.setLastName("Admin");

        mockTypedQuery = Mockito.mock(TypedQuery.class);
        
        // Clear tenant context before each test
        TenantContext.clear();
        
        // Set up the settings service to return the same tenant
        when(settingsService.applyDefaultSettings(Mockito.any(Tenant.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testFindById_whenTenantExists_shouldReturnTenant() {
        Tenant tenant = new Tenant();
        tenant.setTenantKey("find-by-id-tenant-" + UUID.randomUUID());
        tenant.setName("Find By ID Test Tenant");
        tenant.setSchemaName("tenant_find-by-id-" + UUID.randomUUID());
        tenant.setSubdomain("find-test");
        tenant.setDeleted(false);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setBillingPlan("BASIC");
        entityManager.persist(tenant);
        entityManager.flush();
        entityManager.clear();
        Optional<Tenant> result = tenantService.findById(tenant.getId());
        assertTrue(result.isPresent());
        assertEquals(tenant.getTenantKey(), result.get().getTenantKey());
        assertEquals("Find By ID Test Tenant", result.get().getName());
    }

    @Test
    void testFindById_whenTenantDoesNotExist_shouldReturnEmpty() {
        UUID nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        Optional<Tenant> result = tenantService.findById(nonExistentId);
        assertFalse(result.isPresent());
    }

    @Test
    void testCreateTenant_shouldCreateSchemaAndPersistTenant() throws SQLException {
        // Arrange
        String uniqueId = UUID.randomUUID().toString();
        Tenant newTenant = new Tenant();
        newTenant.setTenantKey("new-tenant-" + uniqueId);
        newTenant.setName("New Tenant");
        newTenant.setSubdomain("NewSubdomain-" + uniqueId);
        newTenant.setStatus(TenantStatus.TRIAL);
        newTenant.setBillingPlan("PREMIUM");
        newTenant.setDeleted(false);
        newTenant.setVersion(0);

        TenantAdmin newAdmin = new TenantAdmin();
        newAdmin.setEmail("admin@newtenant-" + uniqueId + ".com");
        newAdmin.setPasswordHash("password123");
        newAdmin.setFirstName("New");
        newAdmin.setLastName("Admin");

        // Act
        tenantService.createTenant(newTenant, newAdmin);

        // Assert
        verify(schemaManager).createSchema("tenant_new-tenant-" + uniqueId);
        assertEquals("newsubdomain-" + uniqueId.toLowerCase(), newTenant.getSubdomain()); // Should be normalized to lowercase
    }
    
    @Test
    void testCreateTenant_schemaCreationFailure_shouldThrowException() throws SQLException {
        // Arrange
        Tenant newTenant = new Tenant();
        newTenant.setTenantKey("new-tenant");
        newTenant.setName("New Tenant");
        newTenant.setSubdomain("NewSubdomain");
        newTenant.setSchemaName("tenant_new-tenant");

        TenantAdmin newAdmin = new TenantAdmin();
        
        doThrow(new SQLException("Failed to create schema")).when(schemaManager).createSchema(anyString());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> tenantService.createTenant(newTenant, newAdmin));
    }
    
    @Test
    void testExecuteInTenantContext_withReturnValue() {
        // Arrange
        String testSchemaName = "test_schema";
        String expectedResult = "test_result";
        
        // Act
        String result = tenantService.executeInTenantContext(testSchemaName, () -> {
            // Verify context was set correctly
            assertEquals(testSchemaName, TenantContext.getCurrentTenant());
            return expectedResult;
        });
        
        // Assert
        assertEquals(expectedResult, result);
        assertNull(TenantContext.getCurrentTenant()); // Context should be cleared after execution
    }
    
    @Test
    void testExecuteInTenantContext_voidVersion() {
        // Arrange
        String testSchemaName = "test_schema";
        AtomicBoolean executed = new AtomicBoolean(false);
        
        // Set some initial context that should be restored
        TenantContext.setCurrentTenant("previous_tenant");
        
        // Act
        tenantService.executeInTenantContext(testSchemaName, () -> {
            // Verify context was set correctly
            assertEquals(testSchemaName, TenantContext.getCurrentTenant());
            executed.set(true);
        });
        
        // Assert
        assertTrue(executed.get());
        assertEquals("previous_tenant", TenantContext.getCurrentTenant()); // Previous context should be restored
    }
    
    @Test
    void testExecuteInTenantContext_preservesPreviousContextAfterExecution() {
        // Arrange
        String previousTenant = "previous_tenant";
        String testSchemaName = "test_schema";
        
        TenantContext.setCurrentTenant(previousTenant);
        
        // Act
        tenantService.executeInTenantContext(testSchemaName, () -> {
            // Just do something in the context
            return "done";
        });
        
        // Assert - should restore previous context
        assertEquals(previousTenant, TenantContext.getCurrentTenant());
    }
    
    @Test
    void testExecuteInTenantContext_clearsContextIfNoPrevious() {
        // Arrange
        String testSchemaName = "test_schema";
        
        // No previous context set
        
        // Act
        tenantService.executeInTenantContext(testSchemaName, () -> {
            // Just do something in the context
            return "done";
        });
        
        // Assert - should clear the context
        assertNull(TenantContext.getCurrentTenant());
    }
    
    @Test
    void testExecuteInTenantContext_handlesExceptions() {
        // Arrange
        String testSchemaName = "test_schema";
        RuntimeException testException = new RuntimeException("Test exception");
        
        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            tenantService.executeInTenantContext(testSchemaName, () -> {
                throw testException;
            });
        });
        
        // Assert it's the same exception and context was cleaned
        assertEquals(testException, thrown);
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void testCreateTenant_withExistingId_shouldThrowException() {
        // Arrange
        UUID existingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Tenant invalidTenant = new Tenant();
        invalidTenant.setId(existingId); // Already has ID
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
        UUID nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        Tenant nonExistentTenant = new Tenant();
        nonExistentTenant.setId(nonExistentId);
        nonExistentTenant.setName("Update Attempt");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.updateTenant(nonExistentTenant));
        assertTrue(exception.getMessage().contains("Tenant not found"));
    }

    @Test
    void testUpdateTenantStatus_shouldChangeStatus() {
        Tenant tenant = new Tenant();
        tenant.setTenantKey("status-test-tenant");
        tenant.setName("Status Test Tenant");
        tenant.setSchemaName("tenant_status-test-tenant");
        tenant.setSubdomain("status-test");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setBillingPlan("BASIC");
        entityManager.persist(tenant);
        entityManager.flush();
        entityManager.clear();
        tenantService.updateTenantStatus(tenant.getId(), TenantStatus.INACTIVE);
        Tenant updated = entityManager.find(Tenant.class, tenant.getId());
        assertEquals(TenantStatus.INACTIVE, updated.getStatus());
    }

    @Test
    void testUpdateTenantStatus_withNonexistentTenant_shouldThrowException() {
        UUID nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.updateTenantStatus(nonExistentId, TenantStatus.INACTIVE));
        assertTrue(exception.getMessage().contains("Tenant not found"));
    }
    
    @Test
    void testDeleteTenant_shouldSetDeletedFlag() {
        Tenant tenant = new Tenant();
        tenant.setTenantKey("delete-test-tenant-" + UUID.randomUUID());
        tenant.setName("Delete Test Tenant");
        tenant.setSchemaName("tenant_delete-test-" + UUID.randomUUID());
        tenant.setSubdomain("delete-test");
        tenant.setDeleted(false);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setBillingPlan("BASIC");
        entityManager.persist(tenant);
        entityManager.flush();
        entityManager.clear();
        tenantService.deleteTenant(tenant.getId());
        Tenant deleted = entityManager.find(Tenant.class, tenant.getId());
        assertTrue(deleted.isDeleted());
    }
    
    @Test
    void testListAll_shouldFilterDeletedTenants() {
        // Arrange
        Tenant activeTenant = new Tenant();
        activeTenant.setTenantKey("active-tenant");
        activeTenant.setName("Active Tenant");
        activeTenant.setSchemaName("tenant_active-tenant");
        activeTenant.setSubdomain("active");
        activeTenant.setDeleted(false);
        activeTenant.setStatus(TenantStatus.ACTIVE);
        activeTenant.setBillingPlan("BASIC");
        entityManager.persist(activeTenant);

        Tenant deletedTenant = new Tenant();
        deletedTenant.setTenantKey("deleted-tenant");
        deletedTenant.setName("Deleted Tenant");
        deletedTenant.setSchemaName("tenant_deleted-tenant");
        deletedTenant.setSubdomain("deleted");
        deletedTenant.setDeleted(true);
        deletedTenant.setStatus(TenantStatus.ACTIVE);
        deletedTenant.setBillingPlan("BASIC");
        entityManager.persist(deletedTenant);
        entityManager.flush();
        entityManager.clear();

        // Act
        List<Tenant> result = tenantService.listAll();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Active Tenant", result.get(0).getName());
    }
}