package dev.tiodati.saas.gocommerce.service;

import dev.tiodati.saas.gocommerce.model.tenant.Tenant;
import dev.tiodati.saas.gocommerce.model.tenant.TenantAdmin;
import dev.tiodati.saas.gocommerce.model.tenant.TenantStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TenantService {

    private static final Logger LOG = Logger.getLogger(TenantService.class);
    
    @Inject
    EntityManager em;
    
    /**
     * Get a tenant by its ID
     */
    public Optional<Tenant> findById(Long id) {
        Tenant tenant = em.find(Tenant.class, id);
        return Optional.ofNullable(tenant);
    }
    
    /**
     * Get a tenant by its unique key
     */
    public Optional<Tenant> findByTenantKey(String tenantKey) {
        try {
            Tenant tenant = em.createQuery(
                    "SELECT t FROM Tenant t WHERE t.tenantKey = :tenantKey", Tenant.class)
                    .setParameter("tenantKey", tenantKey)
                    .getSingleResult();
            return Optional.of(tenant);
        } catch (Exception e) {
            LOG.debug("Tenant not found with key: " + tenantKey);
            return Optional.empty();
        }
    }
    
    /**
     * Get a tenant by its subdomain
     */
    public Optional<Tenant> findBySubdomain(String subdomain) {
        try {
            Tenant tenant = em.createQuery(
                    "SELECT t FROM Tenant t WHERE t.subdomain = :subdomain", Tenant.class)
                    .setParameter("subdomain", subdomain)
                    .getSingleResult();
            return Optional.of(tenant);
        } catch (Exception e) {
            LOG.debug("Tenant not found with subdomain: " + subdomain);
            return Optional.empty();
        }
    }
    
    /**
     * List all tenants
     */
    public List<Tenant> listAll() {
        return em.createQuery("SELECT t FROM Tenant t ORDER BY t.name", Tenant.class)
                .getResultList();
    }
    
    /**
     * Create a new tenant with default schema and admin user
     */
    @Transactional
    public Tenant createTenant(Tenant tenant, TenantAdmin admin) {
        if (tenant.getId() != null) {
            throw new IllegalArgumentException("New tenant cannot have an ID");
        }
        
        // Normalize subdomain to lowercase
        tenant.setSubdomain(tenant.getSubdomain().toLowerCase());
        
        // Generate schema name if not provided
        if (tenant.getSchemaName() == null || tenant.getSchemaName().isEmpty()) {
            tenant.setSchemaName("tenant_" + tenant.getTenantKey().toLowerCase());
        }
        
        try {
            // Persist the tenant
            em.persist(tenant);
            em.flush(); // Ensure ID is generated
            
            // Associate admin with tenant
            admin.setTenant(tenant);
            em.persist(admin);
            
            // Create schema for tenant (would normally create tables within this schema)
            // This would be implemented with a dedicated migration service
            
            LOG.info("Created new tenant: " + tenant.getName() + " with key: " + tenant.getTenantKey());
            return tenant;
        } catch (PersistenceException e) {
            LOG.error("Failed to create tenant: " + tenant.getName(), e);
            throw new RuntimeException("Failed to create tenant: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update an existing tenant
     */
    @Transactional
    public Tenant updateTenant(Tenant tenant) {
        if (tenant.getId() == null) {
            throw new IllegalArgumentException("Cannot update tenant without ID");
        }
        
        Tenant existingTenant = em.find(Tenant.class, tenant.getId());
        if (existingTenant == null) {
            throw new IllegalArgumentException("Tenant not found with ID: " + tenant.getId());
        }
        
        // Cannot change tenant key or schema name after creation
        tenant.setTenantKey(existingTenant.getTenantKey());
        tenant.setSchemaName(existingTenant.getSchemaName());
        
        return em.merge(tenant);
    }
    
    /**
     * Change tenant status
     */
    @Transactional
    public Tenant updateTenantStatus(Long tenantId, TenantStatus status) {
        Tenant tenant = em.find(Tenant.class, tenantId);
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant not found with ID: " + tenantId);
        }
        
        tenant.setStatus(status);
        return tenant;
    }
}