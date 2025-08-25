package dev.tiodati.saas.gocommerce.order.resource;

import java.net.URI;
import java.time.Instant;
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
import dev.tiodati.saas.gocommerce.order.dto.CreateOrderDto;
import dev.tiodati.saas.gocommerce.order.dto.CreateOrderFromCartDto;
import dev.tiodati.saas.gocommerce.order.dto.OrderDto;
import dev.tiodati.saas.gocommerce.order.service.OrderService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST endpoint for managing store orders.
 * Provides comprehensive order lifecycle management including creation, 
 * status updates, cancellation, and tracking.
 */
@Path("/api/v1/stores/{storeId}/orders")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Order Management", description = "Operations for managing store orders")
@RequiresStoreRole({}) // Default store validation for all endpoints
public class OrderResource {

    private final OrderService orderService;

    @Inject
    public OrderResource(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create a new order.
     * 
     * @param storeId The store ID
     * @param createOrderDto Order creation data
     * @return Created order
     */
    @POST
    @Operation(summary = "Create a new order", 
               description = "Creates a new order with items, calculates totals, and updates inventory")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER", "CUSTOMER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN, Roles.CUSTOMER})
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Order created successfully",
                        content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid order data or insufficient stock"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions"),
            @APIResponse(responseCode = "404", description = "Product not found")
    })
    public Response createOrder(
            @PathParam("storeId") UUID storeId,
            @Valid CreateOrderDto createOrderDto) {
        
        Log.infof("Creating order for store %s, customer %s", storeId, createOrderDto.customerId());
        
        try {
            OrderDto createdOrder = orderService.createOrder(storeId, createOrderDto);
            
            return Response.created(URI.create("/api/v1/stores/" + storeId + "/orders/" + createdOrder.id()))
                    .entity(createdOrder)
                    .build();
                    
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid order data: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
                    
        } catch (IllegalStateException e) {
            Log.warnf("Order creation failed: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
                    
        } catch (Exception e) {
            Log.errorf(e, "Error creating order for store %s", storeId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Create a new order from a shopping cart.
     * 
     * @param storeId The store ID
     * @param createOrderFromCartDto Cart-to-order conversion data
     * @return Created order
     */
    @POST
    @Path("/from-cart")
    @Operation(summary = "Create order from shopping cart", 
               description = "Creates a new order from a shopping cart, validates stock, updates inventory, and optionally clears cart")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER", "CUSTOMER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN, Roles.CUSTOMER})
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Order created successfully from cart",
                        content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid cart data, insufficient stock, or cart validation failed"),
            @APIResponse(responseCode = "404", description = "Shopping cart or product not found"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response createOrderFromCart(
            @PathParam("storeId") UUID storeId,
            @Valid CreateOrderFromCartDto createOrderFromCartDto) {
        
        Log.infof("Creating order from cart %s for store %s, customer %s", 
                createOrderFromCartDto.cartId(), storeId, createOrderFromCartDto.customerId());
        
        try {
            OrderDto createdOrder = orderService.createOrderFromCart(storeId, createOrderFromCartDto);
            
            return Response.created(URI.create("/api/v1/stores/" + storeId + "/orders/" + createdOrder.id()))
                    .entity(createdOrder)
                    .build();
                    
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid cart or order data: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
                    
        } catch (IllegalStateException e) {
            Log.warnf("Cart-to-order conversion failed: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
                    
        } catch (Exception e) {
            Log.errorf(e, "Error creating order from cart for store %s", storeId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Get order by ID.
     * 
     * @param storeId The store ID
     * @param orderId The order ID
     * @return Order details
     */
    @GET
    @Path("/{orderId}")
    @Operation(summary = "Get order by ID", 
               description = "Retrieves detailed information about a specific order")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER", "CUSTOMER_SERVICE", "CUSTOMER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN, Roles.CUSTOMER_SERVICE, Roles.CUSTOMER})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order retrieved successfully",
                        content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "404", description = "Order not found"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response getOrder(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId) {
        
        Log.infof("Getting order %s for store %s", orderId, storeId);
        
        try {
            return orderService.findOrder(storeId, orderId)
                    .map(order -> Response.ok(order).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponse("Order not found"))
                            .build());
                            
        } catch (Exception e) {
            Log.errorf(e, "Error retrieving order %s", orderId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * List orders for a store.
     * 
     * @param storeId The store ID
     * @param page Page number (0-based)
     * @param size Page size
     * @param statusId Optional status filter
     * @param customerId Optional customer filter
     * @return List of orders
     */
    @GET
    @Operation(summary = "List orders", 
               description = "Retrieves a paginated list of orders with optional filters")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER", "CUSTOMER_SERVICE"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN, Roles.CUSTOMER_SERVICE})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Orders retrieved successfully",
                        content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response listOrders(
            @PathParam("storeId") UUID storeId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String statusId,
            @QueryParam("customerId") UUID customerId) {
        
        Log.infof("Listing orders for store %s (page=%d, size=%d, status=%s, customerId=%s)", 
                  storeId, page, size, statusId, customerId);
        
        if (page < 0 || size <= 0 || size > 100) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid pagination parameters"))
                    .build();
        }
        
        try {
            List<OrderDto> orders;
            
            if (customerId != null) {
                orders = orderService.getCustomerOrders(storeId, customerId, page, size);
            } else {
                orders = orderService.listOrders(storeId, page, size, statusId);
            }
            
            return Response.ok(orders).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Error listing orders for store %s", storeId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Update order status.
     * 
     * @param storeId The store ID
     * @param orderId The order ID
     * @param statusUpdate Status update data
     * @return Updated order
     */
    @PUT
    @Path("/{orderId}/status")
    @Operation(summary = "Update order status", 
               description = "Updates the status of an order following business rules")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order status updated successfully",
                        content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid status or transition not allowed"),
            @APIResponse(responseCode = "404", description = "Order not found"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response updateOrderStatus(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId,
            Map<String, String> statusUpdate) {
        
        String newStatusId = statusUpdate.get("status");
        if (newStatusId == null || newStatusId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Status is required"))
                    .build();
        }
        
        Log.infof("Updating order %s status to %s for store %s", orderId, newStatusId, storeId);
        
        try {
            return orderService.updateOrderStatus(storeId, orderId, newStatusId)
                    .map(order -> Response.ok(order).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponse("Order not found"))
                            .build());
                            
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid status update: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
                    
        } catch (IllegalStateException e) {
            Log.warnf("Status transition not allowed: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
                    
        } catch (Exception e) {
            Log.errorf(e, "Error updating order %s status", orderId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Cancel an order.
     * 
     * @param storeId The store ID
     * @param orderId The order ID
     * @param cancellationData Cancellation data
     * @return Updated order
     */
    @POST
    @Path("/{orderId}/cancel")
    @Operation(summary = "Cancel an order", 
               description = "Cancels an order and restores inventory if applicable")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER", "CUSTOMER_SERVICE", "CUSTOMER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN, Roles.CUSTOMER_SERVICE, Roles.CUSTOMER})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order cancelled successfully",
                        content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Order cannot be cancelled (already shipped/delivered)"),
            @APIResponse(responseCode = "404", description = "Order not found"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response cancelOrder(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId,
            Map<String, String> cancellationData) {
        
        String reason = cancellationData.getOrDefault("reason", "Cancelled by user");
        
        Log.infof("Cancelling order %s for store %s (reason: %s)", orderId, storeId, reason);
        
        try {
            return orderService.cancelOrder(storeId, orderId, reason)
                    .map(order -> Response.ok(order).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponse("Order not found"))
                            .build());
                            
        } catch (IllegalStateException e) {
            Log.warnf("Order cannot be cancelled: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
                    
        } catch (Exception e) {
            Log.errorf(e, "Error cancelling order %s", orderId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Get order tracking information.
     * 
     * @param storeId The store ID
     * @param orderId The order ID
     * @return Order tracking information
     */
    @GET
    @Path("/{orderId}/tracking")
    @Operation(summary = "Get order tracking information", 
               description = "Retrieves tracking information and status history for an order")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER", "CUSTOMER_SERVICE", "CUSTOMER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN, Roles.CUSTOMER_SERVICE, Roles.CUSTOMER})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Tracking information retrieved successfully"),
            @APIResponse(responseCode = "404", description = "Order not found"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response getOrderTracking(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId) {
        
        Log.infof("Getting tracking info for order %s in store %s", orderId, storeId);
        
        try {
            return orderService.findOrder(storeId, orderId)
                    .map(order -> Response.ok(createTrackingInfo(order)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponse("Order not found"))
                            .build());
                            
        } catch (Exception e) {
            Log.errorf(e, "Error getting tracking info for order %s", orderId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Mark order as shipped.
     * 
     * @param storeId The store ID
     * @param orderId The order ID
     * @param shippingData Shipping data
     * @return Updated order
     */
    @PUT
    @Path("/{orderId}/ship")
    @Operation(summary = "Mark order as shipped", 
               description = "Updates order status to shipped and records shipping date")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order marked as shipped successfully",
                        content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Order cannot be shipped"),
            @APIResponse(responseCode = "404", description = "Order not found"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response markOrderShipped(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId,
            Map<String, String> shippingData) {
        
        Log.infof("Marking order %s as shipped for store %s", orderId, storeId);
        
        try {
            Instant shippedDate = Instant.now();
            return orderService.markOrderShipped(storeId, orderId, shippedDate)
                    .map(order -> Response.ok(order).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponse("Order not found"))
                            .build());
                            
        } catch (IllegalStateException e) {
            Log.warnf("Order cannot be shipped: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
                    
        } catch (Exception e) {
            Log.errorf(e, "Error marking order %s as shipped", orderId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Mark order as delivered.
     * 
     * @param storeId The store ID
     * @param orderId The order ID
     * @param deliveryData Delivery data
     * @return Updated order
     */
    @PUT
    @Path("/{orderId}/deliver")
    @Operation(summary = "Mark order as delivered", 
               description = "Updates order status to delivered and records delivery date")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order marked as delivered successfully",
                        content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Order cannot be delivered"),
            @APIResponse(responseCode = "404", description = "Order not found"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response markOrderDelivered(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId,
            Map<String, String> deliveryData) {
        
        Log.infof("Marking order %s as delivered for store %s", orderId, storeId);
        
        try {
            Instant deliveredDate = Instant.now();
            return orderService.markOrderDelivered(storeId, orderId, deliveredDate)
                    .map(order -> Response.ok(order).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity(new ErrorResponse("Order not found"))
                            .build());
                            
        } catch (IllegalStateException e) {
            Log.warnf("Order cannot be delivered: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
                    
        } catch (Exception e) {
            Log.errorf(e, "Error marking order %s as delivered", orderId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Get orders by date range.
     * 
     * @param storeId The store ID
     * @param startDate Start date (ISO format)
     * @param endDate End date (ISO format)
     * @param page Page number
     * @param size Page size
     * @return List of orders in date range
     */
    @GET
    @Path("/date-range")
    @Operation(summary = "Get orders by date range", 
               description = "Retrieves orders within a specified date range")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER", "CUSTOMER_SERVICE"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN, Roles.CUSTOMER_SERVICE})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Orders retrieved successfully",
                        content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid date parameters"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response getOrdersByDateRange(
            @PathParam("storeId") UUID storeId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        if (startDate == null || endDate == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Start date and end date are required"))
                    .build();
        }
        
        try {
            Instant start = Instant.parse(startDate);
            Instant end = Instant.parse(endDate);
            
            List<OrderDto> orders = orderService.getOrdersByDateRange(storeId, start, end, page, size);
            return Response.ok(orders).build();
            
        } catch (Exception e) {
            Log.errorf(e, "Error getting orders by date range for store %s", storeId);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid date format. Use ISO format (e.g., 2023-01-01T00:00:00Z)"))
                    .build();
        }
    }

    // Helper methods

    /**
     * Creates tracking information from order data.
     */
    private TrackingInfo createTrackingInfo(OrderDto order) {
        return new TrackingInfo(
                order.id(),
                order.orderNumber(),
                order.statusId(),
                order.statusName(),
                order.orderDate(),
                order.shippedDate(),
                order.deliveredDate(),
                order.createdAt(),
                order.updatedAt()
        );
    }

    // Response DTOs

    /**
     * Standard error response.
     */
    public record ErrorResponse(String error) {}

    /**
     * Tracking information response.
     */
    public record TrackingInfo(
            UUID orderId,
            String orderNumber,
            String currentStatus,
            String currentStatusName,
            Instant orderDate,
            Instant shippedDate,
            Instant deliveredDate,
            Instant createdAt,
            Instant updatedAt
    ) {}
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
