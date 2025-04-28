package dev.tiodati.saas.gocommerce.config;

import org.jboss.logging.Logger;

import dev.tiodati.saas.gocommerce.model.tenant.Tenant;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@LookupIfProperty(name = "quarkus.hibernate-orm.multitenant", stringValue = "SCHEMA")
@RequestScoped
@PersistenceUnitExtension("gocommerce")
public class TenantSchemaResolver implements TenantResolver {
    
    private static final Logger LOG = Logger.getLogger(TenantSchemaResolver.class);
    private static final String DEFAULT_TENANT_ID = "default";
    
    @Inject
    CurrentVertxRequest currentVertxRequest;
    
    @Inject
    EntityManager em;
    
    @Override
    public String getDefaultTenantId() {
        return DEFAULT_TENANT_ID;
    }
    
    @Override
    public String resolveTenantId() {
        if (currentVertxRequest == null) {
            LOG.debug("No current Vertx request context available, using default tenant");
            return getDefaultTenantId();
        }
        
        RoutingContext context = currentVertxRequest.getCurrent();
        if (context == null) {
            LOG.debug("No routing context available, using default tenant");
            return getDefaultTenantId();
        }
        
        // First try to get tenant from header
        String tenantKey = context.request().getHeader("X-Tenant");
        if (tenantKey != null && !tenantKey.isEmpty()) {
            LOG.debug("Resolving tenant from X-Tenant header: " + tenantKey);
            String schemaName = getSchemaNameFromTenantKey(tenantKey);
            if (schemaName != null) {
                return schemaName;
            }
        }
        
        // If header not available, try to extract from subdomain
        String host = context.request().getHeader("Host");
        if (host != null && host.contains(".")) {
            String subdomain = host.split("\\.")[0];
            if (!"www".equals(subdomain)) {
                LOG.debug("Resolving tenant from subdomain: " + subdomain);
                String schemaName = getSchemaNameFromSubdomain(subdomain);
                if (schemaName != null) {
                    return schemaName;
                }
            }
        }
        
        LOG.debug("Could not resolve tenant, using default");
        return getDefaultTenantId();
    }
    
    private String getSchemaNameFromTenantKey(String tenantKey) {
        try {
            Tenant tenant = em.createQuery(
                "SELECT t FROM Tenant t WHERE t.tenantKey = :tenantKey", Tenant.class)
                .setParameter("tenantKey", tenantKey)
                .getSingleResult();
            return tenant.getSchemaName();
        } catch (Exception e) {
            LOG.warn("Error resolving schema from tenant key: " + tenantKey, e);
            return null;
        }
    }
    
    private String getSchemaNameFromSubdomain(String subdomain) {
        try {
            Tenant tenant = em.createQuery(
                "SELECT t FROM Tenant t WHERE t.subdomain = :subdomain", Tenant.class)
                .setParameter("subdomain", subdomain)
                .getSingleResult();
            return tenant.getSchemaName();
        } catch (Exception e) {
            LOG.warn("Error resolving schema from subdomain: " + subdomain, e);
            return null;
        }
    }
}