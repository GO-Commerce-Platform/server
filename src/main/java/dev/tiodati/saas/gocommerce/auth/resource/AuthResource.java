package dev.tiodati.saas.gocommerce.auth.resource;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import dev.tiodati.saas.gocommerce.auth.dto.LoginRequest;
import dev.tiodati.saas.gocommerce.auth.dto.RefreshTokenRequest;
import dev.tiodati.saas.gocommerce.auth.dto.TokenResponse;
import dev.tiodati.saas.gocommerce.auth.service.AuthService;
import dev.tiodati.saas.gocommerce.auth.service.KeycloakRoleVerificationService;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import io.quarkus.security.UnauthorizedException;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * REST resource for authentication operations like login, logout, and token
 * refresh. It interacts with the {@link AuthService} to perform these actions.
 */
@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Endpoints for user authentication and session management.")
@ApplicationScoped
public class AuthResource {

    /**
     * Service for handling authentication logic.
     */
    @Inject
    private AuthService authService;

    /**
     * The current JSON Web Token for the authenticated user. Used to access
     * claims and user information.
     */
    @Inject
    private JsonWebToken jwt;

    /**
     * Service for verifying Keycloak roles.
     */
    @Inject
    private KeycloakRoleVerificationService roleVerificationService;

    /**
     * Authenticates a user and returns JWT tokens.
     *
     * @param loginRequest DTO containing username and password.
     * @return Response containing access and refresh tokens.
     */
    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "User Login", description = "Authenticates a user and returns JWT tokens.")
    @APIResponse(responseCode = "200", description = "Login successful", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TokenResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request payload.")
    @APIResponse(responseCode = "401", description = "Authentication failed (invalid credentials).")
    @APIResponse(responseCode = "500", description = "Internal server error during login.")
    public Response login(
            @RequestBody(description = "User credentials for login.", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LoginRequest.class))) @Valid LoginRequest loginRequest) {
        try {
            TokenResponse tokenResponse = authService.login(loginRequest);
            return Response.ok(tokenResponse).build();
        } catch (Exception e) {
            Log.error("Login failed", e);
            // Consider more specific error mapping based on exception type
            return Response.status(Response.Status.UNAUTHORIZED).entity(
                    "{\"error\":\"Login failed: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param refreshTokenRequest DTO containing the refresh token.
     * @return Response containing new access and refresh tokens.
     */
    @POST
    @Path("/refresh")
    @PermitAll // Typically refresh token endpoint is also public but requires a
               // valid refresh token
    @Operation(summary = "Refresh Token", description = "Refreshes an access token using a refresh token.")
    @APIResponse(responseCode = "200", description = "Token refresh successful", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TokenResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request payload or missing refresh token.")
    @APIResponse(responseCode = "401", description = "Token refresh failed (invalid refresh token).")
    @APIResponse(responseCode = "500", description = "Internal server error during token refresh.")
    public Response refreshToken(
            @RequestBody(description = "Refresh token.", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RefreshTokenRequest.class))) @Valid RefreshTokenRequest refreshTokenRequest) {
        try {
            TokenResponse tokenResponse = authService
                    .refreshToken(refreshTokenRequest);
            return Response.ok(tokenResponse).build();
        } catch (Exception e) {
            Log.error("Token refresh failed", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Token refresh failed: "
                            + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Logs out the currently authenticated user. Validates the provided access token
     * and uses the refresh token to invalidate the session.
     *
     * @param authorizationHeader The Authorization header containing the Bearer token.
     * @param refreshTokenRequest DTO containing the refresh token of the user to log out.
     * @return Response indicating logout status.
     */
    @POST
    @Path("/logout")
    @PermitAll // Changed to PermitAll and validate token manually
    @Operation(summary = "User Logout", description = "Logs out the currently authenticated user by invalidating their session.")
    @APIResponse(responseCode = "204", description = "Logout successful.")
    @APIResponse(responseCode = "400", description = "Invalid request or missing refresh token.")
    @APIResponse(responseCode = "401", description = "User not authenticated or refresh token invalid.")
    @APIResponse(responseCode = "500", description = "Internal server error during logout.")
    public Response logout(
            @HeaderParam("Authorization") String authorizationHeader,
            @RequestBody(description = "Refresh token for logout.", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RefreshTokenRequest.class))) @Valid RefreshTokenRequest refreshTokenRequest) {
        
        // Validate the access token first
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Bearer token is missing or malformed.\"}")
                    .build();
        }
        
        String accessToken = authorizationHeader.substring("Bearer ".length());
        if (!authService.validateToken(accessToken)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Access token is invalid or expired.\"}")
                    .build();
        }
        
        // Now proceed with logout using the refresh token
        try {
            authService.logout(refreshTokenRequest.refreshToken());
            return Response.noContent().build(); // HTTP 204 No Content is
                                                 // typical for successful
                                                 // logout
        } catch (Exception e) {
            Log.error("Logout failed", e);
            // Depending on the exception, return appropriate status
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Logout failed: " + e.getMessage()
                            + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint to get current user information based on the JWT.
     *
     * @param securityContext The security context.
     * @return User information or error if not authenticated.
     */
    @GET
    @Path("/me")
    @Authenticated // Ensures only authenticated users can access this
    @Operation(summary = "Get Current User Info", description = "Retrieves information about the currently authenticated user.")
    @APIResponse(responseCode = "200", description = "User information retrieved successfully.")
    @APIResponse(responseCode = "401", description = "User not authenticated.")
    public Response getCurrentUser(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            throw new UnauthorizedException("User not authenticated.");
        }
        // Using the injected JWT to get claims
        String username = jwt.getName();
        String userId = jwt.getSubject();
        Set<String> roles = roleVerificationService.getClientRoles(); // Or
                                                                      // combine
                                                                      // with
                                                                      // realm
                                                                      // roles

        // Construct a simple response DTO or Map
        var userInfo = Map.of("userId", userId != null ? userId : "N/A",
                "username", username != null ? username : "N/A", "roles",
                roles);
        return Response.ok(userInfo).build();
    }

    /**
     * Validates the current session/token. This endpoint can be used by clients
     * to check if the current token is still valid.
     *
     * @param authorizationHeader The Authorization header containing the Bearer
     *                            token.
     * @return Response indicating token validity.
     */
    @GET
    @Path("/validate-token")
    @PermitAll // Or @Authenticated, depending on whether you want to allow
               // unauth check
    @Operation(summary = "Validate Token", description = "Checks if the provided token is valid.")
    @APIResponse(responseCode = "200", description = "Token is valid.")
    @APIResponse(responseCode = "401", description = "Token is invalid or missing.")
    public Response validateToken(
            @HeaderParam("Authorization") String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(
                    "{\"error\":\"Bearer token is missing or malformed.\"}")
                    .build();
        }
        String token = authorizationHeader.substring("Bearer ".length());
        if (authService.validateToken(token)) {
            return Response.ok("{\"message\":\"Token is valid.\"}").build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Token is invalid or expired.\"}")
                    .build();
        }
    }
}
