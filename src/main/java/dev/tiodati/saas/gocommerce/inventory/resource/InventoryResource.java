package dev.tiodati.saas.gocommerce.inventory.resource;

import dev.tiodati.saas.gocommerce.auth.annotation.RequiresStoreRole;
import dev.tiodati.saas.gocommerce.auth.model.Roles;
import dev.tiodati.saas.gocommerce.inventory.dto.InventoryAdjustmentDto;
import dev.tiodati.saas.gocommerce.inventory.dto.InventoryReportDto;
import dev.tiodati.saas.gocommerce.inventory.dto.InventoryUpdateDto;
import dev.tiodati.saas.gocommerce.inventory.dto.LowStockAlertDto;
import dev.tiodati.saas.gocommerce.inventory.service.InventoryService;

import io.quarkus.logging.Log;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for inventory management operations.
 * Provides APIs for stock tracking, adjustments, alerts, and reporting.
 */
@Path("/api/v1/stores/{storeId}/inventory")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Inventory Management", description = "Operations for managing store inventory")
@RequiresStoreRole({}) // Default store validation for all endpoints
public class InventoryResource {

    private final InventoryService inventoryService;

    @Inject
    public InventoryResource(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Update stock levels for a specific product.
     * 
     * @param storeId The store ID
     * @param productId The product ID  
     * @param updateDto Stock update information
     * @return Success response
     */
    @PUT
    @Path("/products/{productId}")
    @Operation(summary = "Update product inventory", 
               description = "Updates stock levels for a specific product with validation and audit trail")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "PRODUCT_MANAGER"})
    @RequiresStoreRole({Roles.PRODUCT_MANAGER, Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Inventory updated successfully"),
            @APIResponse(responseCode = "400", description = "Invalid input or validation error"),
            @APIResponse(responseCode = "404", description = "Product not found"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response updateProductInventory(
            @PathParam("storeId") UUID storeId,
            @PathParam("productId") UUID productId,
            @Valid InventoryUpdateDto updateDto) {
        
        Log.infof("Updating inventory for product %s in store %s", productId, storeId);
        
        // Validate that the product ID in the path matches the DTO
        if (!productId.equals(updateDto.productId())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Product ID in path and body must match"))
                    .build();
        }

        try {
            boolean success = inventoryService.updateProductInventory(storeId, productId, updateDto);
            
            if (success) {
                return Response.ok(new SuccessResponse("Inventory updated successfully", productId)).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Failed to update inventory. Check product exists and data is valid."))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error updating inventory for product %s", productId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Get products with low stock levels.
     * 
     * @param storeId The store ID
     * @param limit Maximum number of results to return
     * @param urgencyLevel Filter by urgency level
     * @return List of low stock alerts
     */
    @GET
    @Path("/low-stock")
    @Operation(summary = "Get low stock alerts", 
               description = "Returns products with low stock levels, optionally filtered by urgency")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "PRODUCT_MANAGER"})
    @RequiresStoreRole({Roles.PRODUCT_MANAGER, Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Low stock alerts retrieved successfully",
                        content = @Content(schema = @Schema(implementation = LowStockAlertDto.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response getLowStockAlerts(
            @PathParam("storeId") UUID storeId,
            @QueryParam("limit") @DefaultValue("50") Integer limit,
            @QueryParam("urgencyLevel") LowStockAlertDto.UrgencyLevel urgencyLevel) {
        
        Log.infof("Getting low stock alerts for store %s (limit=%d, urgency=%s)", 
                  storeId, limit, urgencyLevel);

        try {
            List<LowStockAlertDto> alerts = inventoryService.getLowStockAlerts(storeId, limit, urgencyLevel);
            return Response.ok(alerts).build();
        } catch (Exception e) {
            Log.errorf(e, "Error retrieving low stock alerts for store %s", storeId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Generate comprehensive inventory reports.
     * 
     * @param storeId The store ID
     * @param reportType Type of report to generate
     * @param includeCategoryBreakdown Whether to include category breakdown
     * @return Inventory report with statistics
     */
    @GET
    @Path("/reports")
    @Operation(summary = "Generate inventory report", 
               description = "Generates comprehensive inventory reports with statistics and breakdowns")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "PRODUCT_MANAGER"})
    @RequiresStoreRole({Roles.PRODUCT_MANAGER, Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Inventory report generated successfully",
                        content = @Content(schema = @Schema(implementation = InventoryReportDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid report type"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response getInventoryReports(
            @PathParam("storeId") UUID storeId,
            @QueryParam("reportType") @DefaultValue("SUMMARY") InventoryReportDto.ReportType reportType,
            @QueryParam("includeCategoryBreakdown") @DefaultValue("false") boolean includeCategoryBreakdown) {
        
        Log.infof("Generating inventory report for store %s: type=%s, categoryBreakdown=%s", 
                  storeId, reportType, includeCategoryBreakdown);

        try {
            InventoryReportDto report = inventoryService.generateInventoryReport(
                    storeId, reportType, includeCategoryBreakdown);
            return Response.ok(report).build();
        } catch (Exception e) {
            Log.errorf(e, "Error generating inventory report for store %s", storeId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Record inventory adjustments with audit trail.
     * 
     * @param storeId The store ID
     * @param adjustmentDto Adjustment information
     * @return Success response
     */
    @POST
    @Path("/adjustments")
    @Operation(summary = "Record inventory adjustment", 
               description = "Records inventory adjustments with complete audit trail")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "PRODUCT_MANAGER"})
    @RequiresStoreRole({Roles.PRODUCT_MANAGER, Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Inventory adjustment recorded successfully"),
            @APIResponse(responseCode = "400", description = "Invalid input or would result in negative stock"),
            @APIResponse(responseCode = "404", description = "Product not found"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response recordInventoryAdjustment(
            @PathParam("storeId") UUID storeId,
            @Valid InventoryAdjustmentDto adjustmentDto) {
        
        Log.infof("Recording inventory adjustment for store %s: product=%s, type=%s, quantity=%d",
                  storeId, adjustmentDto.productId(), adjustmentDto.adjustmentType(), adjustmentDto.quantity());

        try {
            boolean success = inventoryService.recordInventoryAdjustment(storeId, adjustmentDto);
            
            if (success) {
                return Response.status(Response.Status.CREATED)
                        .entity(new SuccessResponse("Inventory adjustment recorded successfully", adjustmentDto.productId()))
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Failed to record inventory adjustment. Check product exists and adjustment is valid."))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error recording inventory adjustment for product %s", adjustmentDto.productId());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    // Additional convenience endpoints for enhanced functionality

    /**
     * Get current stock level for a product.
     * 
     * @param storeId The store ID
     * @param productId The product ID
     * @return Current stock level
     */
    @GET
    @Path("/products/{productId}/stock")
    @Operation(summary = "Get current stock level", 
               description = "Returns the current stock level for a specific product")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "PRODUCT_MANAGER", "CUSTOMER_SERVICE"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Stock level retrieved successfully"),
            @APIResponse(responseCode = "404", description = "Product not found"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response getCurrentStockLevel(
            @PathParam("storeId") UUID storeId,
            @PathParam("productId") UUID productId) {
        
        Log.infof("Getting current stock level for product %s in store %s", productId, storeId);

        try {
            Integer stockLevel = inventoryService.getCurrentStockLevel(storeId, productId);
            
            if (stockLevel != null) {
                return Response.ok(new StockLevelResponse(productId, stockLevel)).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Product not found or does not track inventory"))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "Error getting stock level for product %s", productId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    /**
     * Check if product has sufficient stock for a given quantity.
     * 
     * @param storeId The store ID
     * @param productId The product ID
     * @param quantity Required quantity
     * @return Availability check result
     */
    @GET
    @Path("/products/{productId}/availability")
    @Operation(summary = "Check product availability", 
               description = "Checks if a product has sufficient stock for a given quantity")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "PRODUCT_MANAGER", "CUSTOMER_SERVICE", "CUSTOMER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Availability check completed successfully"),
            @APIResponse(responseCode = "400", description = "Invalid quantity"),
            @APIResponse(responseCode = "404", description = "Product not found"),
            @APIResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Response checkProductAvailability(
            @PathParam("storeId") UUID storeId,
            @PathParam("productId") UUID productId,
            @QueryParam("quantity") @DefaultValue("1") Integer quantity) {
        
        Log.infof("Checking availability for product %s in store %s (quantity=%d)", 
                  productId, storeId, quantity);

        if (quantity <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Quantity must be positive"))
                    .build();
        }

        try {
            boolean available = inventoryService.hasSufficientStock(storeId, productId, quantity);
            Integer currentStock = inventoryService.getCurrentStockLevel(storeId, productId);
            
            return Response.ok(new AvailabilityResponse(productId, quantity, available, currentStock)).build();
        } catch (Exception e) {
            Log.errorf(e, "Error checking availability for product %s", productId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Internal server error"))
                    .build();
        }
    }

    // Response DTOs for API responses

    /**
     * Standard success response.
     */
    public record SuccessResponse(String message, UUID resourceId) {}

    /**
     * Standard error response.
     */
    public record ErrorResponse(String error) {}

    /**
     * Stock level response.
     */
    public record StockLevelResponse(UUID productId, Integer stockLevel) {}

    /**
     * Availability check response.
     */
    public record AvailabilityResponse(UUID productId, Integer requestedQuantity, boolean available, Integer currentStock) {}
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
