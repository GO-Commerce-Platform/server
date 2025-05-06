package dev.tiodati.saas.gocommerce.auth.resource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.tiodati.saas.gocommerce.auth.dto.LoginRequest;
import dev.tiodati.saas.gocommerce.auth.dto.RefreshTokenRequest;
import dev.tiodati.saas.gocommerce.auth.dto.TokenResponse;
import dev.tiodati.saas.gocommerce.auth.service.AuthService;
import io.quarkus.logging.Log;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * REST resource for authentication operations
 */
@Path("/api/auth")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Operations for authentication and token management")
public class AuthResource {
    
    @Inject
    AuthService authService;
    
    /**
     * Authenticate a user and get access token
     *
     * @param loginRequest Login credentials
     * @return TokenResponse with access and refresh tokens
     */
    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Authenticate and get tokens", 
               description = "Authenticates a user with username and password and returns access and refresh tokens")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successful authentication"),
        @APIResponse(responseCode = "401", description = "Authentication failed"),
        @APIResponse(responseCode = "400", description = "Invalid request")
    })
    public Response login(@Valid LoginRequest loginRequest) {
        try {
            TokenResponse tokenResponse = authService.login(loginRequest);
            return Response.ok(tokenResponse).build();
        } catch (NotAuthorizedException e) {
            Log.debug("Authentication failed: " + e.getMessage());
            return Response.status(Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Authentication failed: Invalid username or password"))
                    .build();
        } catch (Exception e) {
            Log.error("Authentication error", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Authentication error: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Refresh an access token using a refresh token
     *
     * @param refreshRequest Refresh token request
     * @return TokenResponse with new access and refresh tokens
     */
    @POST
    @Path("/refresh")
    @PermitAll
    @Operation(summary = "Refresh tokens", 
               description = "Get new access and refresh tokens using a refresh token")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Tokens refreshed successfully"),
        @APIResponse(responseCode = "401", description = "Invalid or expired refresh token"),
        @APIResponse(responseCode = "400", description = "Invalid request")
    })
    public Response refresh(@Valid RefreshTokenRequest refreshRequest) {
        try {
            TokenResponse tokenResponse = authService.refreshToken(refreshRequest);
            return Response.ok(tokenResponse).build();
        } catch (NotAuthorizedException e) {
            Log.debug("Token refresh failed: " + e.getMessage());
            return Response.status(Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Token refresh failed: Invalid or expired refresh token"))
                    .build();
        } catch (Exception e) {
            Log.error("Token refresh error", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Token refresh error: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Logout a user by invalidating their refresh token
     *
     * @param refreshToken The refresh token to invalidate
     * @return Success or error response
     */
    @DELETE
    @Path("/logout")
    @PermitAll
    @Operation(summary = "Logout user", 
               description = "Invalidates the refresh token")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successfully logged out"),
        @APIResponse(responseCode = "400", description = "Invalid request")
    })
    public Response logout(@QueryParam("refreshToken") String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Refresh token is required"))
                    .build();
        }
        
        try {
            boolean success = authService.logout(refreshToken);
            if (success) {
                return Response.ok(new SuccessResponse("Successfully logged out")).build();
            } else {
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorResponse("Logout failed"))
                        .build();
            }
        } catch (Exception e) {
            Log.error("Logout error", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Logout error: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Validate a token
     *
     * @param token The token to validate
     * @return Success or error response
     */
    @GET
    @Path("/validate")
    @PermitAll
    @Operation(summary = "Validate token", 
               description = "Checks if a token is valid")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Token is valid"),
        @APIResponse(responseCode = "401", description = "Token is invalid or expired"),
        @APIResponse(responseCode = "400", description = "Invalid request")
    })
    public Response validateToken(@QueryParam("token") String token) {
        if (token == null || token.trim().isEmpty()) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Token is required"))
                    .build();
        }
        
        try {
            boolean valid = authService.validateToken(token);
            if (valid) {
                return Response.ok(new SuccessResponse("Token is valid")).build();
            } else {
                return Response.status(Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Token is invalid or expired"))
                        .build();
            }
        } catch (Exception e) {
            Log.error("Token validation error", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Token validation error: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * Response class for errors
     */
    public static class ErrorResponse {
        private String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
        
        public String getError() {
            return error;
        }
    }
    
    /**
     * Response class for success messages
     */
    public static class SuccessResponse {
        private String message;
        
        public SuccessResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
}