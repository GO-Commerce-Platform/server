package dev.tiodati.saas.gocommerce.store.resource;

import java.util.HashMap;
import java.util.Map;

import dev.tiodati.saas.gocommerce.store.config.StoreSchemaResolver;
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
@Path("/api/store-test")
@ApplicationScoped
public class StoreTestResource {

    @Inject
    StoreSchemaResolver storeResolver; // Now specifying the exact type instead of using TenantResolver

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