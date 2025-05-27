package dev.tiodati.saas.gocommerce.auth.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import dev.tiodati.saas.gocommerce.auth.dto.LoginRequest;
import dev.tiodati.saas.gocommerce.auth.dto.RefreshTokenRequest;
import dev.tiodati.saas.gocommerce.auth.dto.TokenResponse;
import dev.tiodati.saas.gocommerce.auth.model.Roles;
import io.quarkus.logging.Log;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;

/**
 * Service implementation for handling authentication with Keycloak. This
 * service interacts with a Keycloak instance to perform login, logout, token
 * refresh, and token validation operations.
 */
@ApplicationScoped
public class KeycloakAuthService implements AuthService {

    /**
     * Grant type for password-based authentication.
     */
    private static final String GRANT_TYPE_PASSWORD = "password";
    /**
     * Grant type for refreshing an access token.
     */
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    /**
     * REST client for Keycloak token operations.
     */
    @Inject
    @RestClient
    private KeycloakTokenClient keycloakTokenClient;

    /**
     * REST client for Keycloak logout operations.
     */
    @Inject
    @RestClient
    private KeycloakLogoutClient keycloakLogoutClient;

    /**
     * The authentication server URL for Keycloak. Configured via
     * {@code quarkus.oidc.auth-server-url}.
     */
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    private String authServerUrl;

    /**
     * The client ID registered in Keycloak for this application. Configured via
     * {@code quarkus.oidc.client-id}.
     */
    @ConfigProperty(name = "quarkus.oidc.client-id")
    private String clientId;

    /**
     * The client secret for this application, used for confidential clients.
     * Configured via {@code quarkus.oidc.credentials.secret}.
     */
    @ConfigProperty(name = "quarkus.oidc.credentials.secret")
    private String clientSecret;

    /**
     * Service for verifying Keycloak roles from JWT.
     */
    @Inject
    private KeycloakRoleVerificationService roleVerificationService;

    /**
     * Authenticates a user with the given credentials.
     *
     * @param loginRequest The login request containing username and password.
     * @return A {@link TokenResponse} containing access and refresh tokens upon
     *         successful authentication.
     * @throws AuthenticationFailedException if authentication fails.
     */
    @Override
    public TokenResponse login(LoginRequest loginRequest) {
        Form form = new Form().param("grant_type", GRANT_TYPE_PASSWORD)
                .param("client_id", clientId)
                .param("client_secret", clientSecret)
                .param("username", loginRequest.username())
                .param("password", loginRequest.password());

        try {
            Log.infof("Attempting login for user: %s", loginRequest.username());
            TokenResponse tokenResponse = keycloakTokenClient.grantToken(form);

            Log.infof("User %s logged in successfully.",
                    loginRequest.username());
            return tokenResponse; // Simplified, roles might be in JWT directly
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            String errorDetails = response != null
                    ? response.readEntity(String.class)
                    : "No details";
            Log.errorf(e, "Login failed for user %s. Status: %d, Error: %s",
                    loginRequest.username(),
                    response != null ? response.getStatus() : -1, errorDetails);
            throw new AuthenticationFailedException(
                    "Login failed: " + errorDetails, e);
        } catch (Exception e) {
            Log.errorf(e, "Unexpected error during login for user %s.",
                    loginRequest.username());
            throw new AuthenticationFailedException(
                    "Login failed due to an unexpected error.", e);
        }
    }

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param refreshTokenRequest The request containing the refresh token.
     * @return A new {@link TokenResponse} with a new access token.
     * @throws AuthenticationFailedException if token refresh fails.
     */
    @Override
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        Form form = new Form().param("grant_type", GRANT_TYPE_REFRESH_TOKEN)
                .param("client_id", clientId)
                .param("client_secret", clientSecret)
                .param("refresh_token", refreshTokenRequest.refreshToken());

        try {
            Log.info("Attempting to refresh token.");
            TokenResponse tokenResponse = keycloakTokenClient.grantToken(form);
            Log.info("Token refreshed successfully.");
            return tokenResponse; // Simplified
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            String errorDetails = response != null
                    ? response.readEntity(String.class)
                    : "No details";
            Log.errorf(e, "Token refresh failed. Status: %d, Error: %s",
                    response != null ? response.getStatus() : -1, errorDetails);
            throw new AuthenticationFailedException(
                    "Token refresh failed: " + errorDetails, e);
        } catch (Exception e) {
            Log.errorf(e, "Unexpected error during token refresh.");
            throw new AuthenticationFailedException(
                    "Token refresh failed due to an unexpected error.", e);
        }
    }

    /**
     * Logs out a user by invalidating their session in Keycloak.
     *
     * @param refreshToken The refresh token of the user to log out.
     * @return {@code true} if logout was successful or initiated, {@code false}
     *         if an error occurred.
     * @throws AuthenticationFailedException if logout fails (this is less
     *                                       common for logout).
     */
    @Override
    public boolean logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            Log.warn("Logout attempt with no refresh token provided.");
            return false;
        }

        Form form = new Form().param("client_id", clientId)
                .param("client_secret", clientSecret)
                .param("refresh_token", refreshToken);

        // Construct the logout endpoint URL correctly from the authServerUrl
        // Example: authServerUrl = http://localhost:9000/realms/gocommerce
        // We need:
        // http://localhost:9000/realms/gocommerce/protocol/openid-connect/logout
        // This URL is implicitly handled by the @Path on KeycloakLogoutClient
        // and its base URI configuration (quarkus.oidc.auth-server-url).

        try {
            Log.infof(
                    "Attempting logout for user with refresh token (masked).");
            // Use the injected keycloakLogoutClient
            Response keycloakResponse = keycloakLogoutClient.logout(form);

            if (keycloakResponse.getStatusInfo()
                    .getFamily() == Response.Status.Family.SUCCESSFUL) {
                Log.info("User logged out successfully from Keycloak.");
                keycloakResponse.close(); // Close the response
                return true;
            } else {
                String errorDetails = keycloakResponse.hasEntity()
                        ? keycloakResponse.readEntity(String.class)
                        : "No details";
                Log.errorf("Keycloak logout failed. Status: %d, Error: %s",
                        keycloakResponse.getStatus(), errorDetails);
                keycloakResponse.close(); // Close the response
                return false;
            }
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            String errorDetails = response != null && response.hasEntity()
                    ? response.readEntity(String.class)
                    : "No details from WebApplicationException";
            Log.errorf(e,
                    "Logout failed due to WebApplicationException. Status: %d, Error: %s",
                    response != null ? response.getStatus() : -1, errorDetails);
            if (response != null) {
                response.close();
            }
            return false;
        } catch (Exception e) {
            Log.errorf(e, "Unexpected error during logout.");
            return false;
        }
    }

    /**
     * Validates an access token. This is a placeholder. True validation often
     * happens at the resource server via OIDC introspection or JWT signature
     * validation, which Quarkus handles. This method could be used for custom
     * checks if needed.
     *
     * @param accessToken The access token to validate.
     * @return {@code true} if the token is considered valid, {@code false}
     *         otherwise.
     */
    @Override
    public boolean validateToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        // Basic check: Quarkus SecurityIdentity would be non-anonymous if token
        // is valid
        // For more detailed validation, one might inspect the JWT directly
        // or use roleVerificationService if it implies validation.
        return !roleVerificationService.isAnonymous();
    }

    /**
     * Retrieves the roles associated with the currently authenticated user's
     * JWT.
     *
     * @param jwt The JsonWebToken of the current user.
     * @return A set of {@link Roles} for the user.
     * @throws UnauthorizedException if no JWT is present or roles cannot be
     *                               determined.
     */
    public Set<Roles> getUserRoles(JsonWebToken jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new UnauthorizedException("No authenticated user found.");
        }
        // Assuming client roles are the primary source for application Roles
        Set<String> clientRoleStrings = roleVerificationService
                .getClientRoles(jwt, clientId);
        if (clientRoleStrings.isEmpty()) {
            // Fallback or also check realm roles if necessary
            clientRoleStrings = roleVerificationService.getRealmRoles(jwt);
        }

        // This pipeline is now correct because Roles.fromRoleName returns
        // Optional<Roles>
        return clientRoleStrings.stream().map(Roles::fromRoleName) // Stream<Optional<Roles>>
                .filter(Optional::isPresent) // Stream<Optional<Roles>>
                                             // (non-empty)
                .map(Optional::get) // Stream<Roles>
                .collect(Collectors.toSet());
    }
}
