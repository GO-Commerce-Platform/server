package dev.tiodati.saas.gocommerce.customer.resource;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.tiodati.saas.gocommerce.auth.annotation.RequiresStoreRole;
import dev.tiodati.saas.gocommerce.auth.model.Roles;
import dev.tiodati.saas.gocommerce.customer.dto.CreateCustomerDto;
import dev.tiodati.saas.gocommerce.customer.dto.CustomerDto;
import dev.tiodati.saas.gocommerce.customer.entity.CustomerStatus;
import dev.tiodati.saas.gocommerce.customer.service.CustomerService;
import io.quarkus.logging.Log;
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
 * REST endpoint for managing store customers.
 * Provides comprehensive customer management functionality including
 * registration,
 * profile management, status updates, and customer search capabilities.
 */
@Path("/api/v1/stores/{storeId}/customers")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Customers", description = "Operations for managing store customers")
@RequiresStoreRole({}) // Default store validation for all endpoints
public class CustomerResource {

    /**
     * Service for customer management, handling CRUD operations and business logic.
     */
    private final CustomerService customerService;

    @Inject
    public CustomerResource(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Get a list of customers for a store.
     *
     * @param storeId The store ID
     * @param page    Page number for pagination (0-based)
     * @param size    Page size for pagination
     * @param status  Optional status filter
     * @return List of customers
     */
    @GET
    @Operation(summary = "List customers", description = "Retrieves a paginated list of customers for a store")
    @RolesAllowed({ "PLATFORM_ADMIN", "STORE_ADMIN", "CUSTOMER_SERVICE" })
    @RequiresStoreRole({ Roles.CUSTOMER_SERVICE, Roles.STORE_ADMIN })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "List of customers retrieved successfully", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
            @APIResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
            @APIResponse(responseCode = "404", description = "Store not found")
    })
    public List<CustomerDto> listCustomers(
            @PathParam("storeId") UUID storeId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") CustomerStatus status) {
        Log.infof("Listing customers for store %s (page=%d, size=%d, status=%s)",
                storeId, page, size, status);
        return customerService.listCustomers(storeId, page, size, status);
    }

    /**
     * Get a specific customer by ID.
     *
     * @param storeId    The store ID
     * @param customerId The customer ID
     * @return The customer details
     */
    @GET
    @Path("/{customerId}")
    @Operation(summary = "Get customer by ID", description = "Retrieves a specific customer by ID")
    @RolesAllowed({ "PLATFORM_ADMIN", "STORE_ADMIN", "CUSTOMER_SERVICE", "CUSTOMER" })
    @RequiresStoreRole({ Roles.CUSTOMER_SERVICE, Roles.STORE_ADMIN, Roles.CUSTOMER })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Customer retrieved successfully", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
            @APIResponse(responseCode = "404", description = "Customer not found"),
            @APIResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public Response getCustomer(
            @PathParam("storeId") UUID storeId,
            @PathParam("customerId") UUID customerId) {
        Log.infof("Getting customer %s for store %s", customerId, storeId);
        return customerService.findCustomer(storeId, customerId)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Register a new customer.
     * This endpoint supports both admin-initiated registration and
     * self-registration.
     *
     * @param storeId     The store ID
     * @param customerDto The customer registration data
     * @return The created customer
     */
    @POST
    @Operation(summary = "Register new customer", description = "Creates a new customer account")
    @RolesAllowed({ "PLATFORM_ADMIN", "STORE_ADMIN", "CUSTOMER_SERVICE", "CUSTOMER" }) // Allow self-registration with 'CUSTOMER' role
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Customer created successfully", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid input or email already exists"),
            @APIResponse(responseCode = "409", description = "Customer with this email already exists")
    })
    public Response registerCustomer(
            @PathParam("storeId") UUID storeId,
            @Valid CreateCustomerDto customerDto) {
        Log.infof("Registering customer for store %s: %s", storeId, customerDto.email());
        try {
            // Check if customer already exists
            var existingCustomer = customerService.findCustomerByEmail(storeId, customerDto.email());
            if (existingCustomer.isPresent()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Customer with this email already exists")
                        .build();
            }

            CustomerDto createdCustomer = customerService.createCustomer(storeId, customerDto);
            return Response
                    .created(URI.create(String.format("/api/v1/stores/%s/customers/%s", storeId, createdCustomer.id())))
                    .entity(createdCustomer)
                    .build();
        } catch (Exception e) {
            Log.error("Error creating customer", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error creating customer: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Update an existing customer's profile.
     *
     * @param storeId     The store ID
     * @param customerId  The customer ID
     * @param customerDto Updated customer data
     * @return The updated customer
     */
    @PUT
    @Path("/{customerId}")
    @Operation(summary = "Update customer profile", description = "Updates an existing customer's profile information")
    @RolesAllowed({ "PLATFORM_ADMIN", "STORE_ADMIN", "CUSTOMER_SERVICE", "CUSTOMER" })
    @RequiresStoreRole({ Roles.CUSTOMER_SERVICE, Roles.STORE_ADMIN, Roles.CUSTOMER })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Customer updated successfully", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
            @APIResponse(responseCode = "404", description = "Customer not found"),
            @APIResponse(responseCode = "400", description = "Invalid input"),
            @APIResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public Response updateCustomer(
            @PathParam("storeId") UUID storeId,
            @PathParam("customerId") UUID customerId,
            @Valid CustomerDto customerDto) {
        Log.infof("Updating customer %s for store %s", customerId, storeId);

        // Ensure the ID in the path matches the ID in the DTO
        if (!customerId.equals(customerDto.id())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Customer ID in path does not match ID in request body")
                    .build();
        }

        try {
            return customerService.updateCustomer(storeId, customerDto)
                    .map(updatedCustomer -> Response.ok(updatedCustomer).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (Exception e) {
            Log.error("Error updating customer", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error updating customer: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Update a customer's account status.
     *
     * @param storeId    The store ID
     * @param customerId The customer ID
     * @param status     The new status
     * @return The updated customer
     */
    @PUT
    @Path("/{customerId}/status")
    @Operation(summary = "Update customer status", description = "Updates a customer's account status")
    @RolesAllowed({ "PLATFORM_ADMIN", "STORE_ADMIN", "CUSTOMER_SERVICE" })
    @RequiresStoreRole({ Roles.CUSTOMER_SERVICE, Roles.STORE_ADMIN })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Customer status updated successfully", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
            @APIResponse(responseCode = "404", description = "Customer not found"),
            @APIResponse(responseCode = "400", description = "Invalid status"),
            @APIResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public Response updateCustomerStatus(
            @PathParam("storeId") UUID storeId,
            @PathParam("customerId") UUID customerId,
            @QueryParam("status") CustomerStatus status) {
        Log.infof("Updating status for customer %s in store %s to %s", customerId, storeId, status);

        if (status == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Status parameter is required")
                    .build();
        }

        try {
            return customerService.updateCustomerStatus(storeId, customerId, status)
                    .map(updatedCustomer -> Response.ok(updatedCustomer).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (Exception e) {
            Log.error("Error updating customer status", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error updating customer status: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Search customers by name or email.
     *
     * @param storeId The store ID
     * @param query   The search query
     * @param page    Page number for pagination (0-based)
     * @param size    Page size for pagination
     * @return List of matching customers
     */
    @GET
    @Path("/search")
    @Operation(summary = "Search customers", description = "Search customers by name or email")
    @RolesAllowed({ "PLATFORM_ADMIN", "STORE_ADMIN", "CUSTOMER_SERVICE" })
    @RequiresStoreRole({ Roles.CUSTOMER_SERVICE, Roles.STORE_ADMIN })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Search results retrieved successfully", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
            @APIResponse(responseCode = "400", description = "Invalid search query"),
            @APIResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public Response searchCustomers(
            @PathParam("storeId") UUID storeId,
            @QueryParam("q") String query,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        Log.infof("Searching customers for store %s with query: %s (page=%d, size=%d)",
                storeId, query, page, size);

        if (query == null || query.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Search query parameter 'q' is required")
                    .build();
        }

        try {
            List<CustomerDto> results = customerService.searchCustomers(storeId, query.trim(), page, size);
            return Response.ok(results).build();
        } catch (Exception e) {
            Log.error("Error searching customers", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error searching customers: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get customer count by status.
     *
     * @param storeId The store ID
     * @param status  The customer status
     * @return Customer count
     */
    @GET
    @Path("/count")
    @Operation(summary = "Get customer count", description = "Get count of customers by status")
    @RolesAllowed({ "PLATFORM_ADMIN", "STORE_ADMIN", "CUSTOMER_SERVICE" })
    @RequiresStoreRole({ Roles.CUSTOMER_SERVICE, Roles.STORE_ADMIN })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Customer count retrieved successfully"),
            @APIResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public Response getCustomerCount(
            @PathParam("storeId") UUID storeId,
            @QueryParam("status") CustomerStatus status) {
        Log.infof("Getting customer count for store %s with status: %s", storeId, status);

        try {
            long count = customerService.countCustomersByStatus(storeId, status);
            return Response.ok().entity("{\"count\": " + count + "}").build();
        } catch (Exception e) {
            Log.error("Error getting customer count", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error getting customer count: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Find customer by email address.
     *
     * @param storeId The store ID
     * @param email   The customer email
     * @return The customer details
     */
    @GET
    @Path("/by-email")
    @Operation(summary = "Find customer by email", description = "Retrieves a customer by email address")
    @RolesAllowed({ "PLATFORM_ADMIN", "STORE_ADMIN", "CUSTOMER_SERVICE" })
    @RequiresStoreRole({ Roles.CUSTOMER_SERVICE, Roles.STORE_ADMIN })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Customer retrieved successfully", content = @Content(schema = @Schema(implementation = CustomerDto.class))),
            @APIResponse(responseCode = "404", description = "Customer not found"),
            @APIResponse(responseCode = "400", description = "Email parameter is required"),
            @APIResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public Response getCustomerByEmail(
            @PathParam("storeId") UUID storeId,
            @QueryParam("email") String email) {
        Log.infof("Finding customer by email %s for store %s", email, storeId);

        if (email == null || email.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Email parameter is required")
                    .build();
        }

        try {
            return customerService.findCustomerByEmail(storeId, email.trim())
                    .map(dto -> Response.ok(dto).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (Exception e) {
            Log.error("Error finding customer by email", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error finding customer: " + e.getMessage())
                    .build();
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
