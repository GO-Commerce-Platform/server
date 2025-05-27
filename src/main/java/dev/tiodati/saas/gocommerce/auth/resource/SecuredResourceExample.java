package dev.tiodati.saas.gocommerce.auth.resource;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Example resource demonstrating different levels of security with role-based access control.
 */
@Path("/api/secured-example")
@RequestScoped
public class SecuredResourceExample {

    @GET
    @Path("/public")
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public Response publicEndpoint() {
        return Response.ok("This is a public endpoint, accessible by anyone").build();
    }

    @GET
    @Path("/authenticated")
    @RolesAllowed({ "customer" })
    @Produces(MediaType.TEXT_PLAIN)
    public Response authenticatedEndpoint() {
        return Response.ok("This endpoint requires authentication").build();
    }

    @GET
    @Path("/customer")
    @RolesAllowed({ "customer" })
    @Produces(MediaType.TEXT_PLAIN)
    public Response customerEndpoint() {
        return Response.ok("This endpoint requires CUSTOMER role").build();
    }

    @GET
    @Path("/store-admin")
    @RolesAllowed({ "store_admin" })
    @Produces(MediaType.TEXT_PLAIN)
    public Response storeAdminEndpoint() {
        return Response.ok("This endpoint requires STORE_ADMIN role").build();
    }

    @GET
    @Path("/platform-admin")
    @RolesAllowed({ "platform_admin" })
    @Produces(MediaType.TEXT_PLAIN)
    public Response platformAdminEndpoint() {
        return Response.ok("This endpoint requires PLATFORM_ADMIN role").build();
    }

    @GET
    @Path("/mixed-access")
    @RolesAllowed({ "customer" })
    @Produces(MediaType.APPLICATION_JSON)
    public Response mixedAccessEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "You have access to:");

        // Everyone gets customer features
        response.put("customerFeatures", "Customer features are available");

        // Add admin features if the user has those roles (this is illustrative)
        // In a real implementation, you would use the PermissionValidator to check roles
        if (true) { // hasStoreAdminRole()
            response.put("storeAdminFeatures", "Store administration features");
        }

        if (true) { // hasPlatformAdminRole()
            response.put("platformAdminFeatures", "Platform administration features");
        }

        return Response.ok(response).build();
    }
}
