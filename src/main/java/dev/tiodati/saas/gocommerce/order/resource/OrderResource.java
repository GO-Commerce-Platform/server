package dev.tiodati.saas.gocommerce.order.resource;

import java.net.URI;
import java.util.List;
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

import java.time.Instant;
import java.util.Map;

/**
 * REST endpoint for managing store orders.
 */
@Path("/api/v1/stores/{storeId}/orders")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Orders", description = "Operations for managing store orders")
@RequiresStoreRole({}) // Default store validation for all endpoints
public class OrderResource {

    /**
     * Service for order management, handling CRUD operations and business logic.
     */
    private final OrderService orderService;

    @Inject
    public OrderResource(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Get a list of orders for a store.
     *
     * @param storeId  The store ID
     * @param page     Page number for pagination
     * @param size     Page size for pagination  
     * @param statusId Optional status ID to filter by
     * @return List of orders
     */
    @GET
    @Operation(summary = "List orders", description = "Retrieves a list of orders for a store")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "List of orders retrieved successfully", content = @Content(schema = @Schema(implementation = OrderDto.class)))
    })
    public List<OrderDto> listOrders(
            @PathParam("storeId") UUID storeId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("statusId") String statusId) {
        Log.infof("Listing orders for store %s (page=%d, size=%d, statusId=%s)",
                storeId, page, size, statusId);
        return orderService.listOrders(storeId, page, size, statusId);
    }

    /**
     * Get a specific order by ID.
     *
     * @param storeId The store ID
     * @param orderId The order ID
     * @return The order details
     */
    @GET
    @Path("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieves a specific order by ID")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER", "CUSTOMER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order retrieved successfully", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "404", description = "Order not found")
    })
    public Response getOrder(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId) {
        Log.infof("Getting order %s for store %s", orderId, storeId);
        return orderService.findOrder(storeId, orderId)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Create a new order from a shopping cart.
     * This endpoint converts a shopping cart into an order, validating stock availability
     * and calculating totals.
     *
     * @param storeId              The store ID
     * @param createOrderFromCartDto The order creation data with cart and shipping/billing info
     * @return The created order
     */
    @POST
    @Path("/from-cart")
    @Operation(summary = "Create order from cart", description = "Creates a new order from a shopping cart with shipping and billing information")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "CUSTOMER", "ORDER_MANAGER"})
    @RequiresStoreRole({Roles.STORE_ADMIN}) // Specific roles required for order creation
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Order created successfully", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid input - cart not found, empty, or insufficient stock"),
            @APIResponse(responseCode = "401", description = "Unauthorized - customer doesn't own the cart"),
            @APIResponse(responseCode = "409", description = "Conflict - business logic validation failed (e.g., insufficient stock)")
    })
    public Response createOrderFromCart(
            @PathParam("storeId") UUID storeId,
            @Valid CreateOrderFromCartDto createOrderFromCartDto) {
        Log.infof("Creating order from cart for store %s: cartId=%s, customerId=%s", 
                storeId, createOrderFromCartDto.cartId(), createOrderFromCartDto.customerId());
        try {
            OrderDto createdOrder = orderService.createOrderFromCart(storeId, createOrderFromCartDto);
            return Response
                    .created(URI.create(String.format("/api/v1/stores/%s/orders/%s", storeId, createdOrder.id())))
                    .entity(createdOrder)
                    .build();
        } catch (IllegalArgumentException e) {
            Log.warnf("Bad request creating order from cart: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (IllegalStateException e) {
            Log.warnf("Business logic error creating order from cart: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (SecurityException e) {
            Log.warnf("Unauthorized creating order from cart: %s", e.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            Log.error("Unexpected error creating order from cart", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error creating order\"}")
                    .build();
        }
    }

    /**
     * Get orders for a specific customer.
     *
     * @param storeId    The store ID
     * @param customerId The customer ID
     * @param page       Page number for pagination
     * @param size       Page size for pagination
     * @return List of customer orders
     */
    @GET
    @Path("/customer/{customerId}")
    @Operation(summary = "Get customer orders", description = "Retrieves orders for a specific customer")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "CUSTOMER", "ORDER_MANAGER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Customer orders retrieved successfully", content = @Content(schema = @Schema(implementation = OrderDto.class)))
    })
    public List<OrderDto> getCustomerOrders(
            @PathParam("storeId") UUID storeId,
            @PathParam("customerId") UUID customerId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        Log.infof("Getting orders for customer %s in store %s (page=%d, size=%d)",
                customerId, storeId, page, size);
        return orderService.getCustomerOrders(storeId, customerId, page, size);
    }

    /**
     * Get an order by its order number.
     *
     * @param storeId     The store ID
     * @param orderNumber The order number
     * @return The order details
     */
    @GET
    @Path("/number/{orderNumber}")
    @Operation(summary = "Get order by number", description = "Retrieves a specific order by its order number")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER", "CUSTOMER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order retrieved successfully", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "404", description = "Order not found")
    })
    public Response getOrderByNumber(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderNumber") String orderNumber) {
        Log.infof("Getting order by number %s for store %s", orderNumber, storeId);
        return orderService.findOrderByNumber(storeId, orderNumber)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Update order status.
     * Updates the status of an existing order following business rules for status transitions.
     *
     * @param storeId     The store ID
     * @param orderId     The order ID
     * @param statusUpdate Request body containing the new status
     * @return The updated order
     */
    @PUT
    @Path("/{orderId}/status")
    @Operation(summary = "Update order status", description = "Updates the status of an order following business workflow rules")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order status updated successfully", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid status or illegal status transition"),
            @APIResponse(responseCode = "404", description = "Order not found"),
            @APIResponse(responseCode = "409", description = "Conflict - status transition not allowed")
    })
    public Response updateOrderStatus(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId,
            Map<String, String> statusUpdate) {
        Log.infof("Updating status for order %s in store %s to %s", orderId, storeId, statusUpdate.get("status"));
        
        String newStatus = statusUpdate.get("status");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Status is required\"}")
                    .build();
        }
        
        try {
            return orderService.updateOrderStatus(storeId, orderId, newStatus.trim())
                    .map(dto -> Response.ok(dto).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\": \"Order not found\"}")
                            .build());
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid status update request: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (IllegalStateException e) {
            Log.warnf("Status transition not allowed: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            Log.error("Unexpected error updating order status", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error updating order status\"}")
                    .build();
        }
    }

    /**
     * Cancel an order.
     * Cancels an order and restores inventory if applicable.
     *
     * @param storeId           The store ID
     * @param orderId           The order ID
     * @param cancellationRequest Request body containing cancellation reason
     * @return The cancelled order
     */
    @POST
    @Path("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancels an order and restores inventory if applicable")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER", "CUSTOMER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order cancelled successfully", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid cancellation request or order cannot be cancelled"),
            @APIResponse(responseCode = "404", description = "Order not found"),
            @APIResponse(responseCode = "409", description = "Conflict - order cannot be cancelled in current status")
    })
    public Response cancelOrder(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId,
            Map<String, String> cancellationRequest) {
        Log.infof("Cancelling order %s in store %s", orderId, storeId);
        
        String reason = cancellationRequest != null ? cancellationRequest.get("reason") : null;
        if (reason == null || reason.trim().isEmpty()) {
            reason = "Cancelled by user request";
        }
        
        try {
            return orderService.cancelOrder(storeId, orderId, reason.trim())
                    .map(dto -> Response.ok(dto).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\": \"Order not found\"}")
                            .build());
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid cancellation request: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (IllegalStateException e) {
            Log.warnf("Order cannot be cancelled: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            Log.error("Unexpected error cancelling order", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error cancelling order\"}")
                    .build();
        }
    }

    /**
     * Mark an order as shipped.
     *
     * @param storeId The store ID
     * @param orderId The order ID
     * @param shippingInfo Request body containing shipping information
     * @return The updated order
     */
    @POST
    @Path("/{orderId}/ship")
    @Operation(summary = "Mark order as shipped", description = "Marks an order as shipped with shipping date and tracking information")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order marked as shipped successfully", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid request or order cannot be shipped"),
            @APIResponse(responseCode = "404", description = "Order not found"),
            @APIResponse(responseCode = "409", description = "Conflict - order cannot be shipped in current status")
    })
    public Response markOrderShipped(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId,
            Map<String, Object> shippingInfo) {
        Log.infof("Marking order %s as shipped in store %s", orderId, storeId);
        
        try {
            Instant shippedDate = shippingInfo != null && shippingInfo.get("shippedDate") != null 
                ? Instant.parse(shippingInfo.get("shippedDate").toString())
                : Instant.now();
                
            return orderService.markOrderShipped(storeId, orderId, shippedDate)
                    .map(dto -> Response.ok(dto).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\": \"Order not found\"}")
                            .build());
        } catch (IllegalStateException e) {
            Log.warnf("Order cannot be shipped: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            Log.error("Unexpected error marking order as shipped", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error marking order as shipped\"}")
                    .build();
        }
    }

    /**
     * Mark an order as delivered.
     *
     * @param storeId The store ID
     * @param orderId The order ID
     * @param deliveryInfo Request body containing delivery information
     * @return The updated order
     */
    @POST
    @Path("/{orderId}/deliver")
    @Operation(summary = "Mark order as delivered", description = "Marks an order as delivered with delivery date")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER"})
    @RequiresStoreRole({Roles.ORDER_MANAGER, Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order marked as delivered successfully", content = @Content(schema = @Schema(implementation = OrderDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid request or order cannot be delivered"),
            @APIResponse(responseCode = "404", description = "Order not found"),
            @APIResponse(responseCode = "409", description = "Conflict - order cannot be delivered in current status")
    })
    public Response markOrderDelivered(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId,
            Map<String, Object> deliveryInfo) {
        Log.infof("Marking order %s as delivered in store %s", orderId, storeId);
        
        try {
            Instant deliveredDate = deliveryInfo != null && deliveryInfo.get("deliveredDate") != null 
                ? Instant.parse(deliveryInfo.get("deliveredDate").toString())
                : Instant.now();
                
            return orderService.markOrderDelivered(storeId, orderId, deliveredDate)
                    .map(dto -> Response.ok(dto).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\": \"Order not found\"}")
                            .build());
        } catch (IllegalStateException e) {
            Log.warnf("Order cannot be delivered: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            Log.error("Unexpected error marking order as delivered", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error marking order as delivered\"}")
                    .build();
        }
    }

    /**
     * Get orders within a date range.
     *
     * @param storeId The store ID
     * @param startDate Start date for filtering
     * @param endDate End date for filtering
     * @param page Page number for pagination
     * @param size Page size for pagination
     * @return List of orders within the date range
     */
    @GET
    @Path("/date-range")
    @Operation(summary = "Get orders by date range", description = "Retrieves orders within a specified date range")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(schema = @Schema(implementation = OrderDto.class)))
    })
    public List<OrderDto> getOrdersByDateRange(
            @PathParam("storeId") UUID storeId,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        Log.infof("Getting orders for store %s from %s to %s (page=%d, size=%d)",
                storeId, startDate, endDate, page, size);
        
        Instant start = startDate != null ? Instant.parse(startDate) : Instant.now().minus(java.time.Duration.ofDays(30));
        Instant end = endDate != null ? Instant.parse(endDate) : Instant.now();
        
        return orderService.getOrdersByDateRange(storeId, start, end, page, size);
    }

    /**
     * Get count of orders by status.
     *
     * @param storeId The store ID
     * @param statusId The status to count
     * @return Count of orders with the specified status
     */
    @GET
    @Path("/count")
    @Operation(summary = "Count orders by status", description = "Returns the count of orders for a specific status")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order count retrieved successfully")
    })
    public Response countOrdersByStatus(
            @PathParam("storeId") UUID storeId,
            @QueryParam("statusId") String statusId) {
        Log.infof("Counting orders for store %s with status %s", storeId, statusId);
        
        if (statusId == null || statusId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"StatusId is required\"}")
                    .build();
        }
        
        long count = orderService.countOrdersByStatus(storeId, statusId.trim());
        return Response.ok(Map.of("statusId", statusId, "count", count)).build();
    }

    /**
     * Get order tracking information.
     * Provides detailed tracking information including status history and progress.
     *
     * @param storeId The store ID
     * @param orderId The order ID
     * @return Order tracking information
     */
    @GET
    @Path("/{orderId}/tracking")
    @Operation(summary = "Get order tracking", description = "Retrieves detailed tracking information for an order")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "ORDER_MANAGER", "CUSTOMER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Order tracking information retrieved successfully"),
            @APIResponse(responseCode = "404", description = "Order not found")
    })
    public Response getOrderTracking(
            @PathParam("storeId") UUID storeId,
            @PathParam("orderId") UUID orderId) {
        Log.infof("Getting tracking info for order %s in store %s", orderId, storeId);
        
        try {
            return orderService.findOrder(storeId, orderId)
                    .map(order -> {
                        // Create tracking information from order data
                        Map<String, Object> trackingInfo = Map.of(
                                "orderId", order.id(),
                                "orderNumber", order.orderNumber(),
                                "status", order.statusId(),
                                "statusName", order.statusName(),
                                "orderDate", order.orderDate(),
                                "shippedDate", order.shippedDate(),
                                "deliveredDate", order.deliveredDate(),
                                "estimatedDelivery", calculateEstimatedDelivery(order),
                                "trackingSteps", generateTrackingSteps(order)
                        );
                        return Response.ok(trackingInfo).build();
                    })
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\": \"Order not found\"}")
                            .build());
        } catch (Exception e) {
            Log.error("Unexpected error getting order tracking", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error getting order tracking\"}")
                    .build();
        }
    }

    /**
     * Calculate estimated delivery date based on order status and shipping method.
     * This is a placeholder implementation that can be enhanced with real business logic.
     */
    private Instant calculateEstimatedDelivery(OrderDto order) {
        if (order.deliveredDate() != null) {
            return order.deliveredDate();
        }
        if (order.shippedDate() != null) {
            // Estimate 3-5 business days from ship date
            return order.shippedDate().plus(java.time.Duration.ofDays(5));
        }
        if (order.orderDate() != null) {
            // Estimate 7-10 business days from order date
            return order.orderDate().plus(java.time.Duration.ofDays(10));
        }
        return null;
    }

    /**
     * Generate tracking steps based on order status.
     * This is a placeholder implementation that can be enhanced with real tracking data.
     */
    private List<Map<String, Object>> generateTrackingSteps(OrderDto order) {
        List<Map<String, Object>> steps = new java.util.ArrayList<>();
        
        if (order.orderDate() != null) {
            steps.add(Map.of(
                    "status", "PENDING",
                    "description", "Order placed",
                    "timestamp", order.orderDate(),
                    "completed", true
            ));
        }
        
        // Add more steps based on order status
        switch (order.statusId()) {
            case "CONFIRMED":
            case "PROCESSING":
            case "SHIPPED":
            case "DELIVERED":
                steps.add(Map.of(
                        "status", "CONFIRMED",
                        "description", "Order confirmed",
                        "timestamp", order.orderDate(),
                        "completed", true
                ));
        }
        
        if ("PROCESSING".equals(order.statusId()) || "SHIPPED".equals(order.statusId()) || "DELIVERED".equals(order.statusId())) {
            steps.add(Map.of(
                    "status", "PROCESSING",
                    "description", "Order being processed",
                    "timestamp", order.orderDate(),
                    "completed", true
            ));
        }
        
        if (order.shippedDate() != null) {
            steps.add(Map.of(
                    "status", "SHIPPED",
                    "description", "Order shipped",
                    "timestamp", order.shippedDate(),
                    "completed", true
            ));
        }
        
        if (order.deliveredDate() != null) {
            steps.add(Map.of(
                    "status", "DELIVERED",
                    "description", "Order delivered",
                    "timestamp", order.deliveredDate(),
                    "completed", true
            ));
        }
        
        return steps;
    }
}
