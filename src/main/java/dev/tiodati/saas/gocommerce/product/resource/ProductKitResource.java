package dev.tiodati.saas.gocommerce.product.resource;

import dev.tiodati.saas.gocommerce.product.dto.CreateProductKitDto;
import dev.tiodati.saas.gocommerce.product.dto.UpdateProductKitDto;
import dev.tiodati.saas.gocommerce.product.service.ProductKitService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/stores/{storeId}/kits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductKitResource {

    private final ProductKitService productKitService;

    @Inject
    public ProductKitResource(ProductKitService productKitService) {
        this.productKitService = productKitService;
    }

    @GET
    @RolesAllowed({"platform-admin", "store-admin", "product-manager", "customer-service", "customer"})
    public Response list(@PathParam("storeId") UUID storeId) {
        return Response.ok(productKitService.findByStoreId(storeId)).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"platform-admin", "store-admin", "product-manager", "customer-service", "customer"})
    public Response findById(@PathParam("id") UUID id) {
        return productKitService.findById(id)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @RolesAllowed({"platform-admin", "store-admin", "product-manager"})
    public Response create(@PathParam("storeId") UUID storeId, @Valid CreateProductKitDto createProductKitDto) {
        return Response.status(Response.Status.CREATED)
                .entity(productKitService.create(storeId, createProductKitDto))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"platform-admin", "store-admin", "product-manager"})
    public Response update(@PathParam("id") UUID id, @Valid UpdateProductKitDto updateProductKitDto) {
        return Response.ok(productKitService.update(id, updateProductKitDto)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"platform-admin", "store-admin"})
    public Response delete(@PathParam("id") UUID id) {
        productKitService.delete(id);
        return Response.noContent().build();
    }
}
