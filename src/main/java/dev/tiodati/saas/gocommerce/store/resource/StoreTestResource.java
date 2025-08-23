package dev.tiodati.saas.gocommerce.store.resource;

import java.util.HashMap;
import java.util.Map;

import dev.tiodati.saas.gocommerce.store.config.UnifiedTenantResolver;
import io.quarkus.hibernate.orm.PersistenceUnitExtension; // Import the qualifier
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource for testing store functionality and tenant resolution.
 */
@Path("/api/v1/store-test")
@ApplicationScoped
public class StoreTestResource {

    /**
     * Unified resolver for store schemas that works in both production and test environments.
     * It is qualified with {@link PersistenceUnitExtension} to ensure it uses the correct
     * persistence unit.
     */
    @Inject
    @PersistenceUnitExtension
    private UnifiedTenantResolver storeResolver;

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStoreInfo() {
        Log.info("Getting store info");

        Map<String, Object> info = new HashMap<>();
        info.put("currentTenantId", storeResolver.resolveTenantId());
        info.put("defaultTenantId", storeResolver.getDefaultTenantId());

        return Response.ok(info).build();
    }
}
