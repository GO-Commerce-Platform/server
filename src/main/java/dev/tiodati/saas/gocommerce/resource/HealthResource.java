package dev.tiodati.saas.gocommerce.resource;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/health")
@ApplicationScoped
@Tag(name = "Health", description = "Health check endpoints")
public class HealthResource {

    private static final Logger LOG = Logger.getLogger(HealthResource.class);
    
    @Inject
    EntityManager em;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Health check", description = "Returns the health status of the application")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            // Try to access tenant table to verify database connectivity
            long tenantCount = (long) em.createQuery("SELECT COUNT(t) FROM Tenant t").getSingleResult();
            response.put("database", Map.of(
                "status", "UP",
                "tenantCount", tenantCount
            ));
        } catch (Exception e) {
            LOG.error("Database health check failed", e);
            response.put("database", Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
            response.put("status", "DEGRADED");
        }
        
        return response;
    }
}
