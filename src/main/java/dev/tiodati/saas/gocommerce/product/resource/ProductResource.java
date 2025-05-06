package dev.tiodati.saas.gocommerce.product.resource;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import io.quarkus.logging.Log;

import dev.tiodati.saas.gocommerce.auth.annotation.RequiresStoreRole;
import dev.tiodati.saas.gocommerce.auth.model.Roles;
import dev.tiodati.saas.gocommerce.auth.service.PermissionValidator;
import dev.tiodati.saas.gocommerce.product.dto.CreateProductDto;
import dev.tiodati.saas.gocommerce.product.dto.ProductDto;
import dev.tiodati.saas.gocommerce.product.service.ProductService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
// import jakarta.ws.rs.NotFoundException; // Not used
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST endpoint for managing store products.
 */
@Path("/api/stores/{storeId}/products")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Products", description = "Operations for managing store products")
@RequiresStoreRole({}) // Default store validation for all endpoints
public class ProductResource {

    // private static final Logger LOG = Logger.getLogger(ProductResource.class);
    
    private final ProductService productService;
    private final PermissionValidator permissionValidator;
    
    @Inject
    public ProductResource(ProductService productService, PermissionValidator permissionValidator) {
        this.productService = productService;
        this.permissionValidator = permissionValidator;
    }
    
    /**
     * Get a list of products for a store
     * 
     * @param storeId The store ID
     * @param page Page number for pagination
     * @param size Page size for pagination
     * @param categoryId Optional category ID to filter by
     * @return List of products
     */
    @GET
    @Operation(summary = "List products", description = "Retrieves a list of products for a store")
    @RolesAllowed({"admin", "store-admin", "user", "product-manager"})
    @APIResponses({
        @APIResponse(responseCode = "200", description = "List of products retrieved successfully",
                     content = @Content(schema = @Schema(implementation = ProductDto.class)))
    })
    public List<ProductDto> listProducts(
            @PathParam("storeId") UUID storeId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("categoryId") UUID categoryId) {
        Log.infof("Listing products for store %s (page=%d, size=%d, categoryId=%s)", 
            storeId, page, size, categoryId);
        return productService.listProducts(storeId, page, size, categoryId);
    }
    
    /**
     * Get a specific product by ID
     * 
     * @param storeId The store ID
     * @param productId The product ID
     * @return The product details
     */
    @GET
    @Path("/{productId}")
    @Operation(summary = "Get product by ID", description = "Retrieves a specific product by ID")
    @RolesAllowed({"admin", "store-admin", "user", "product-manager"})
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Product retrieved successfully",
                     content = @Content(schema = @Schema(implementation = ProductDto.class))),
        @APIResponse(responseCode = "404", description = "Product not found")
    })
    public Response getProduct(
            @PathParam("storeId") UUID storeId,
            @PathParam("productId") UUID productId) {
        Log.infof("Getting product %s for store %s", productId, storeId);
        return productService.findProduct(storeId, productId)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    /**
     * Create a new product for a store
     * 
     * @param storeId The store ID
     * @param productDto The product data
     * @return The created product
     */
    @POST
    @Operation(summary = "Create product", description = "Creates a new product for a store")
    @RolesAllowed({"admin", "store-admin", "product-manager"})
    @RequiresStoreRole({Roles.PRODUCT_MANAGER, Roles.STORE_ADMIN}) // Specific roles required
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Product created successfully"),
        @APIResponse(responseCode = "400", description = "Invalid input")
    })
    public Response createProduct(
            @PathParam("storeId") UUID storeId,
            @Valid CreateProductDto productDto) {
        Log.infof("Creating product for store %s: %s", storeId, productDto.name());
        try {
            ProductDto createdProduct = productService.createProduct(storeId, productDto);
            return Response.created(URI.create(String.format("/api/stores/%s/products/%s", storeId, createdProduct.id())))
                    .entity(createdProduct).build();
        } catch (Exception e) {
            Log.error("Error creating product", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating product").build();
        }
    }
    
    /**
     * Update an existing product
     * 
     * @param storeId The store ID
     * @param productId The product ID
     * @param productDto The updated product data
     * @return The updated product
     */
    @PUT
    @Path("/{productId}")
    @Operation(summary = "Update product", description = "Updates an existing product")
    @RolesAllowed({"admin", "store-admin", "product-manager"})
    @RequiresStoreRole({Roles.PRODUCT_MANAGER, Roles.STORE_ADMIN}) // Specific roles required
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Product updated successfully"),
        @APIResponse(responseCode = "400", description = "Invalid input"),
        @APIResponse(responseCode = "404", description = "Product not found")
    })
    public Response updateProduct(
            @PathParam("storeId") UUID storeId,
            @PathParam("productId") UUID productId,
            @Valid ProductDto productDto) {
        Log.infof("Updating product %s for store %s", productId, storeId);
        if (!productId.equals(productDto.id())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Product ID mismatch").build();
        }
        
        try {
            return productService.updateProduct(storeId, productDto)
                    .map(dto -> Response.ok(dto).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (Exception e) {
            Log.error("Error updating product", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error updating product").build();
        }
    }
    
    /**
     * Delete a product
     * 
     * @param storeId The store ID
     * @param productId The product ID
     * @return Success response
     */
    @DELETE
    @Path("/{productId}")
    @Operation(summary = "Delete product", description = "Deletes a product (soft delete)")
    @RolesAllowed({"admin", "store-admin", "product-manager"})
    @RequiresStoreRole({Roles.PRODUCT_MANAGER, Roles.STORE_ADMIN}) // Specific roles required
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Product deleted successfully"),
        @APIResponse(responseCode = "404", description = "Product not found")
    })
    public Response deleteProduct(
            @PathParam("storeId") UUID storeId,
            @PathParam("productId") UUID productId) {
        Log.infof("Deleting product %s for store %s", productId, storeId);
        try {
            if (productService.deleteProduct(storeId, productId)) {
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            Log.error("Error deleting product", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error deleting product").build();
        }
    }
    
    /**
     * Bulk operation - update product inventory
     * 
     * @param storeId The store ID
     * @param inventoryUpdates Map of product IDs to new stock quantities
     * @return Success response
     */
    @PUT
    @Path("/inventory")
    @Operation(summary = "Update inventory", description = "Bulk update product inventory levels")
    @RolesAllowed({"admin", "store-admin", "product-manager"})
    @RequiresStoreRole({Roles.PRODUCT_MANAGER, Roles.STORE_ADMIN}) // Specific roles required
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Inventory updated successfully"),
        @APIResponse(responseCode = "400", description = "Invalid input")
    })
    public Response updateInventory(
            @PathParam("storeId") UUID storeId,
            Map<UUID, Integer> inventoryUpdates) {
        
        try {
            // Example of programmatic security check
            boolean isAdmin = permissionValidator.hasRole(Roles.PLATFORM_ADMIN);
            int maxUpdatesAllowed = isAdmin ? Integer.MAX_VALUE : 100;
            
            if (inventoryUpdates.size() > maxUpdatesAllowed) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Exceeded maximum allowed inventory updates"))
                        .build();
            }
            
            productService.updateInventory(storeId, inventoryUpdates);
            return Response.ok(Map.of("success", true, "updated", inventoryUpdates.size())).build();
        } catch (Exception e) {
            Log.error("Error updating inventory", e);
            return Response.serverError()
                    .entity(Map.of("error", "Failed to update inventory: " + e.getMessage()))
                    .build();
        }
    }
}