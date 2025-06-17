package dev.tiodati.saas.gocommerce.cart.resource;

import java.net.URI;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.tiodati.saas.gocommerce.cart.dto.CreateCartItemDto;
import dev.tiodati.saas.gocommerce.cart.dto.UpdateCartItemDto;
import dev.tiodati.saas.gocommerce.cart.service.CartService;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

/**
 * REST endpoint for shopping cart operations.
 * Provides HTTP API for cart and cart item management.
 */
@Path("/api/v1/carts")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Shopping Cart", description = "Operations for managing shopping carts")
@RequiredArgsConstructor
public class CartResource {

    /**
     * Service for handling cart operations.
     */
    private final CartService cartService;

    @GET
    @Path("/customer/{customerId}")
    @Operation(summary = "Get or create cart for customer", description = "Retrieves the active cart for a customer or creates a new one if none exists")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @APIResponse(responseCode = "404", description = "Customer not found")
    })
    public Response getOrCreateCartForCustomer(@PathParam("customerId") UUID customerId) {
        Log.infof("REST: Getting cart for customer %s", customerId);

        var cart = cartService.getOrCreateCart(customerId);
        return Response.ok(cart).build();
    }

    @GET
    @Path("/session/{sessionId}")
    @Operation(summary = "Get or create guest cart", description = "Retrieves the cart for a guest session or creates a new one if none exists")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Guest cart retrieved successfully")
    })
    public Response getOrCreateGuestCart(@PathParam("sessionId") String sessionId) {
        Log.infof("REST: Getting guest cart for session %s", sessionId);

        var cart = cartService.getOrCreateGuestCart(sessionId);
        return Response.ok(cart).build();
    }

    @GET
    @Path("/{cartId}")
    @Operation(summary = "Get cart by ID", description = "Retrieves a specific cart by its ID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Cart found"),
            @APIResponse(responseCode = "404", description = "Cart not found")
    })
    public Response getCart(@PathParam("cartId") UUID cartId) {
        Log.infof("REST: Getting cart %s", cartId);

        return cartService.getCartById(cartId)
                .map(cart -> Response.ok(cart).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/{cartId}/summary")
    @Operation(summary = "Get cart summary", description = "Retrieves a summary of cart totals and item count")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Cart summary retrieved"),
            @APIResponse(responseCode = "404", description = "Cart not found")
    })
    public Response getCartSummary(@PathParam("cartId") UUID cartId) {
        Log.infof("REST: Getting cart summary for %s", cartId);

        return cartService.getCartSummary(cartId)
                .map(summary -> Response.ok(summary).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/{cartId}/items")
    @Operation(summary = "Add item to cart", description = "Adds a product to the cart or increases quantity if already present")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Item added to cart"),
            @APIResponse(responseCode = "400", description = "Invalid request"),
            @APIResponse(responseCode = "404", description = "Cart or product not found")
    })
    public Response addItemToCart(
            @PathParam("cartId") UUID cartId,
            @Valid CreateCartItemDto createItemDto) {
        Log.infof("REST: Adding item to cart %s", cartId);

        try {
            var cartItem = cartService.addItemToCart(cartId, createItemDto);
            return Response.created(URI.create("/api/v1/carts/" + cartId + "/items/" + cartItem.id()))
                    .entity(cartItem)
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error: " + e.getMessage())
                    .build();
        }
    }

    @PUT
    @Path("/{cartId}/items/{itemId}")
    @Operation(summary = "Update cart item", description = "Updates the quantity of an item in the cart")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Item updated successfully"),
            @APIResponse(responseCode = "400", description = "Invalid request"),
            @APIResponse(responseCode = "404", description = "Cart or item not found")
    })
    public Response updateCartItem(
            @PathParam("cartId") UUID cartId,
            @PathParam("itemId") UUID itemId,
            @Valid UpdateCartItemDto updateItemDto) {
        Log.infof("REST: Updating item %s in cart %s", itemId, cartId);

        try {
            var cartItem = cartService.updateCartItem(cartId, itemId, updateItemDto);
            return Response.ok(cartItem).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{cartId}/items/{itemId}")
    @Operation(summary = "Remove item from cart", description = "Removes a specific item from the cart")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Item removed successfully"),
            @APIResponse(responseCode = "400", description = "Invalid request"),
            @APIResponse(responseCode = "404", description = "Cart or item not found")
    })
    public Response removeItemFromCart(
            @PathParam("cartId") UUID cartId,
            @PathParam("itemId") UUID itemId) {
        Log.infof("REST: Removing item %s from cart %s", itemId, cartId);

        try {
            cartService.removeItemFromCart(cartId, itemId);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{cartId}/items")
    @Operation(summary = "Clear cart", description = "Removes all items from the cart")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Cart cleared successfully"),
            @APIResponse(responseCode = "404", description = "Cart not found")
    })
    public Response clearCart(@PathParam("cartId") UUID cartId) {
        Log.infof("REST: Clearing cart %s", cartId);

        try {
            cartService.clearCart(cartId);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/transfer")
    @Operation(summary = "Transfer guest cart to customer", description = "Associates a guest cart with a customer account")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Cart transferred successfully"),
            @APIResponse(responseCode = "404", description = "Guest cart not found")
    })
    public Response transferGuestCartToCustomer(
            @QueryParam("sessionId") String sessionId,
            @QueryParam("customerId") UUID customerId) {
        Log.infof("REST: Transferring guest cart from session %s to customer %s", sessionId, customerId);

        return cartService.transferGuestCartToCustomer(sessionId, customerId)
                .map(cart -> Response.ok(cart).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/{cartId}/items")
    @Operation(summary = "Get cart items", description = "Retrieves all items in the cart")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Cart items retrieved successfully"),
            @APIResponse(responseCode = "404", description = "Cart not found")
    })
    public Response getCartItems(@PathParam("cartId") UUID cartId) {
        Log.infof("REST: Getting items for cart %s", cartId);

        var items = cartService.getCartItems(cartId);
        return Response.ok(items).build();
    }

    @GET
    @Path("/customer/{customerId}/item-count")
    @Operation(summary = "Get cart item count", description = "Gets the total number of items in customer's active cart")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Item count retrieved successfully")
    })
    public Response getCartItemCount(@PathParam("customerId") UUID customerId) {
        Log.infof("REST: Getting item count for customer %s", customerId);

        var count = cartService.getCartItemCount(customerId);
        return Response.ok(count).build();
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
