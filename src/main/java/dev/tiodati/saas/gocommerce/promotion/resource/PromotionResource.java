package dev.tiodati.saas.gocommerce.promotion.resource;

import dev.tiodati.saas.gocommerce.auth.annotation.RequiresStoreRole;
import dev.tiodati.saas.gocommerce.auth.model.Roles;
import dev.tiodati.saas.gocommerce.promotion.dto.CreatePromotionDto;
import dev.tiodati.saas.gocommerce.promotion.dto.PromotionDto;
import dev.tiodati.saas.gocommerce.promotion.dto.UpdatePromotionDto;
import dev.tiodati.saas.gocommerce.promotion.entity.PromotionConfig;
import dev.tiodati.saas.gocommerce.promotion.service.PromotionService;
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

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST endpoint for managing store promotions and discounts.
 */
@Path("/api/v1/stores/{storeId}/promotions")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Promotions", description = "Operations for managing store promotions and discounts")
@RequiresStoreRole({}) // Default store validation for all endpoints
public class PromotionResource {

    private final PromotionService promotionService;

    @Inject
    public PromotionResource(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    /**
     * Get all promotions for a store.
     *
     * @param storeId The store ID
     * @return List of promotions
     */
    @GET
    @Operation(summary = "List promotions", description = "Retrieves all promotions for a store")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "PROMOTION_MANAGER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Promotions retrieved successfully", 
                        content = @Content(schema = @Schema(implementation = PromotionDto.class)))
    })
    public List<PromotionDto> listPromotions(@PathParam("storeId") UUID storeId) {
        Log.infof("Listing promotions for store %s", storeId);
        return promotionService.getStorePromotions(storeId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Get a specific promotion by ID.
     *
     * @param storeId     The store ID
     * @param promotionId The promotion ID
     * @return The promotion details
     */
    @GET
    @Path("/{promotionId}")
    @Operation(summary = "Get promotion by ID", description = "Retrieves a specific promotion by ID")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "PROMOTION_MANAGER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Promotion retrieved successfully", 
                        content = @Content(schema = @Schema(implementation = PromotionDto.class))),
            @APIResponse(responseCode = "404", description = "Promotion not found")
    })
    public Response getPromotion(@PathParam("storeId") UUID storeId,
                                @PathParam("promotionId") UUID promotionId) {
        Log.infof("Getting promotion %s for store %s", promotionId, storeId);
        return promotionService.getStorePromotions(storeId)
                .stream()
                .filter(p -> p.getId().equals(promotionId))
                .findFirst()
                .map(this::mapToDto)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Create a new promotion.
     *
     * @param storeId           The store ID
     * @param createPromotionDto The promotion creation data
     * @return The created promotion
     */
    @POST
    @Operation(summary = "Create promotion", description = "Creates a new promotion for a store")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN"})
    @RequiresStoreRole({Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Promotion created successfully", 
                        content = @Content(schema = @Schema(implementation = PromotionDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid promotion data"),
            @APIResponse(responseCode = "409", description = "Promotion code already exists")
    })
    public Response createPromotion(@PathParam("storeId") UUID storeId,
                                   @Valid CreatePromotionDto createPromotionDto) {
        Log.infof("Creating promotion for store %s: code=%s, name=%s", 
                storeId, createPromotionDto.code(), createPromotionDto.name());
        try {
            // Check if promotion code already exists for this store
            if (promotionService.findPromotionByCode(storeId, createPromotionDto.code()).isPresent()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\": \"Promotion code already exists for this store\"}")
                        .build();
            }

            var promotion = mapToEntity(createPromotionDto, storeId);
            var createdPromotion = promotionService.createPromotion(promotion);
            var responseDto = mapToDto(createdPromotion);

            return Response
                    .created(URI.create(String.format("/api/v1/stores/%s/promotions/%s", 
                            storeId, createdPromotion.getId())))
                    .entity(responseDto)
                    .build();
        } catch (IllegalArgumentException e) {
            Log.warnf("Bad request creating promotion: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            Log.error("Unexpected error creating promotion", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error creating promotion\"}")
                    .build();
        }
    }

    /**
     * Update an existing promotion.
     *
     * @param storeId           The store ID
     * @param promotionId       The promotion ID
     * @param updatePromotionDto The promotion update data
     * @return The updated promotion
     */
    @PUT
    @Path("/{promotionId}")
    @Operation(summary = "Update promotion", description = "Updates an existing promotion")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN"})
    @RequiresStoreRole({Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Promotion updated successfully", 
                        content = @Content(schema = @Schema(implementation = PromotionDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid promotion data"),
            @APIResponse(responseCode = "404", description = "Promotion not found")
    })
    public Response updatePromotion(@PathParam("storeId") UUID storeId,
                                   @PathParam("promotionId") UUID promotionId,
                                   @Valid UpdatePromotionDto updatePromotionDto) {
        Log.infof("Updating promotion %s for store %s", promotionId, storeId);
        try {
            // Find existing promotion
            var existingPromotion = promotionService.getStorePromotions(storeId)
                    .stream()
                    .filter(p -> p.getId().equals(promotionId))
                    .findFirst();
            
            if (existingPromotion.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Promotion not found\"}")
                        .build();
            }

            // Update the promotion
            var promotion = existingPromotion.get();
            updatePromotionFromDto(promotion, updatePromotionDto);
            var updatedPromotion = promotionService.updatePromotion(promotion);
            var responseDto = mapToDto(updatedPromotion);

            return Response.ok(responseDto).build();
        } catch (IllegalArgumentException e) {
            Log.warnf("Bad request updating promotion: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            Log.error("Unexpected error updating promotion", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error updating promotion\"}")
                    .build();
        }
    }

    /**
     * Delete/deactivate a promotion.
     *
     * @param storeId     The store ID
     * @param promotionId The promotion ID
     * @return Success response
     */
    @DELETE
    @Path("/{promotionId}")
    @Operation(summary = "Delete promotion", description = "Deactivates a promotion (soft delete)")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN"})
    @RequiresStoreRole({Roles.STORE_ADMIN})
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Promotion deactivated successfully"),
            @APIResponse(responseCode = "404", description = "Promotion not found")
    })
    public Response deletePromotion(@PathParam("storeId") UUID storeId,
                                   @PathParam("promotionId") UUID promotionId) {
        Log.infof("Deactivating promotion %s for store %s", promotionId, storeId);
        try {
            boolean deactivated = promotionService.deactivatePromotion(promotionId);
            if (deactivated) {
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Promotion not found\"}")
                        .build();
            }
        } catch (Exception e) {
            Log.error("Unexpected error deactivating promotion", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error deactivating promotion\"}")
                    .build();
        }
    }

    /**
     * Get promotion by code.
     *
     * @param storeId The store ID
     * @param code    The promotion code
     * @return The promotion details
     */
    @GET
    @Path("/code/{code}")
    @Operation(summary = "Get promotion by code", description = "Retrieves a promotion by its code")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "PROMOTION_MANAGER", "CUSTOMER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Promotion retrieved successfully", 
                        content = @Content(schema = @Schema(implementation = PromotionDto.class))),
            @APIResponse(responseCode = "404", description = "Promotion not found")
    })
    public Response getPromotionByCode(@PathParam("storeId") UUID storeId,
                                      @PathParam("code") String code) {
        Log.infof("Getting promotion by code '%s' for store %s", code, storeId);
        return promotionService.findPromotionByCode(storeId, code)
                .map(this::mapToDto)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Calculate discount for an order amount using the best available promotion.
     *
     * @param storeId     The store ID
     * @param orderAmount The order amount
     * @return Discount information
     */
    @GET
    @Path("/discount/best")
    @Operation(summary = "Calculate best discount", description = "Calculates the best available discount for an order amount")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "PROMOTION_MANAGER", "CUSTOMER", "ORDER_MANAGER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Best discount calculated successfully")
    })
    public Response calculateBestDiscount(@PathParam("storeId") UUID storeId,
                                         @QueryParam("orderAmount") BigDecimal orderAmount,
                                         @QueryParam("promotionCodes") List<String> promotionCodes) {
        Log.infof("Calculating best discount for store %s with order amount %.2f and codes %s", 
                storeId, orderAmount.doubleValue(), promotionCodes);
        
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Order amount must be greater than 0\"}")
                    .build();
        }

        try {
            var discountAmount = promotionService.calculateBestDiscount(storeId, orderAmount, 
                    promotionCodes != null ? promotionCodes : List.of());
            
            var response = Map.of(
                    "orderAmount", orderAmount,
                    "discountAmount", discountAmount,
                    "finalAmount", orderAmount.subtract(discountAmount)
            );
            
            return Response.ok(response).build();
        } catch (Exception e) {
            Log.error("Unexpected error calculating discount", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error calculating discount\"}")
                    .build();
        }
    }

    /**
     * Get applicable volume promotions for an order amount.
     *
     * @param storeId     The store ID
     * @param orderAmount The order amount
     * @return List of applicable promotions
     */
    @GET
    @Path("/volume-based")
    @Operation(summary = "Get volume promotions", description = "Retrieves volume-based promotions applicable to an order amount")
    @RolesAllowed({"PLATFORM_ADMIN", "STORE_ADMIN", "PROMOTION_MANAGER", "CUSTOMER", "ORDER_MANAGER"})
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Volume promotions retrieved successfully", 
                        content = @Content(schema = @Schema(implementation = PromotionDto.class)))
    })
    public List<PromotionDto> getVolumePromotions(@PathParam("storeId") UUID storeId,
                                                 @QueryParam("orderAmount") BigDecimal orderAmount) {
        Log.infof("Getting volume promotions for store %s with order amount %.2f", 
                storeId, orderAmount != null ? orderAmount.doubleValue() : 0.0);
        
        if (orderAmount == null) {
            orderAmount = BigDecimal.ZERO;
        }

        return promotionService.findApplicableVolumePromotions(storeId, orderAmount)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    // Helper methods for mapping between entities and DTOs

    private PromotionDto mapToDto(PromotionConfig promotion) {
        return new PromotionDto(
                promotion.getId(),
                promotion.getStoreId(),
                promotion.getCode(),
                promotion.getName(),
                promotion.getDescription(),
                promotion.getType(),
                promotion.getDiscountValue(),
                promotion.getMinimumOrderAmount(),
                promotion.getMaximumDiscountAmount(),
                promotion.getValidFrom(),
                promotion.getValidUntil(),
                promotion.getUsageLimit(),
                promotion.getUsageCount(),
                promotion.getActive(),
                promotion.getPriority(),
                promotion.getCreatedAt(),
                promotion.getUpdatedAt(),
                promotion.getVersion()
        );
    }

    private PromotionConfig mapToEntity(CreatePromotionDto dto, UUID storeId) {
        return PromotionConfig.builder()
                .storeId(storeId)
                .code(dto.code())
                .name(dto.name())
                .description(dto.description())
                .type(dto.type())
                .discountValue(dto.discountValue())
                .minimumOrderAmount(dto.minimumOrderAmount())
                .maximumDiscountAmount(dto.maximumDiscountAmount())
                .validFrom(dto.validFrom())
                .validUntil(dto.validUntil())
                .usageLimit(dto.usageLimit())
                .active(dto.active())
                .priority(dto.priority())
                .build();
    }

    private void updatePromotionFromDto(PromotionConfig promotion, UpdatePromotionDto dto) {
        if (dto.code() != null) promotion.setCode(dto.code());
        if (dto.name() != null) promotion.setName(dto.name());
        if (dto.description() != null) promotion.setDescription(dto.description());
        if (dto.type() != null) promotion.setType(dto.type());
        if (dto.discountValue() != null) promotion.setDiscountValue(dto.discountValue());
        if (dto.minimumOrderAmount() != null) promotion.setMinimumOrderAmount(dto.minimumOrderAmount());
        if (dto.maximumDiscountAmount() != null) promotion.setMaximumDiscountAmount(dto.maximumDiscountAmount());
        if (dto.validFrom() != null) promotion.setValidFrom(dto.validFrom());
        if (dto.validUntil() != null) promotion.setValidUntil(dto.validUntil());
        if (dto.usageLimit() != null) promotion.setUsageLimit(dto.usageLimit());
        if (dto.active() != null) promotion.setActive(dto.active());
        if (dto.priority() != null) promotion.setPriority(dto.priority());
    }
}
