package dev.tiodati.saas.gocommerce.auth.service;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import dev.tiodati.saas.gocommerce.auth.dto.LoginRequest;
import dev.tiodati.saas.gocommerce.auth.dto.RefreshTokenRequest;
import dev.tiodati.saas.gocommerce.auth.dto.TokenResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;

/**
 * Implementation of AuthService using Keycloak
 */
@ApplicationScoped
public class KeycloakAuthService implements AuthService {

    private static final Logger LOG = Logger.getLogger(KeycloakAuthService.class);
    
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;
    
    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;
    
    @ConfigProperty(name = "quarkus.oidc.credentials.secret")
    String clientSecret;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public TokenResponse login(LoginRequest loginRequest) {
        try {
            // Remove "/realms/{realm}" from the end to get the base URL
            String keycloakBaseUrl = authServerUrl.substring(0, authServerUrl.lastIndexOf("/realms/"));
            
            // Extract realm name from the auth server URL
            String realm = authServerUrl.substring(authServerUrl.lastIndexOf("/") + 1);
            
            // Create Keycloak token endpoint URL
            String tokenEndpoint = keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
            
            // Build the REST client for Keycloak
            KeycloakTokenClient tokenClient = RestClientBuilder.newBuilder()
                    .baseUri(URI.create(tokenEndpoint))
                    .build(KeycloakTokenClient.class);
            
            // Prepare the form data for token request
            Form form = new Form()
                    .param("username", loginRequest.username())
                    .param("password", loginRequest.password())
                    .param("grant_type", "password")
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret);
            
            // Send the token request to Keycloak
            Response response = tokenClient.getToken(form);
            
            if (response.getStatus() == 200) {
                Map<String, Object> tokenData = response.readEntity(Map.class);
                
                // Extract token information
                String accessToken = (String) tokenData.get("access_token");
                String refreshToken = (String) tokenData.get("refresh_token");
                String tokenType = (String) tokenData.get("token_type");
                int expiresIn = (Integer) tokenData.get("expires_in");
                
                // Extract roles from the access token
                List<String> roles = extractRolesFromToken(accessToken);
                
                return new TokenResponse(accessToken, refreshToken, tokenType, expiresIn, roles);
            } else {
                // This is an expected authentication failure, log at debug level
                LOG.debug("Authentication failed with status: " + response.getStatus());
                throw new NotAuthorizedException("Authentication failed: " + response.getStatusInfo().getReasonPhrase());
            }
        } catch (WebApplicationException e) {
            // Specific web exceptions like 401 should be logged at debug level since they're expected
            LOG.debug("Authentication failed: " + e.getMessage());
            throw new NotAuthorizedException("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            // Unexpected exceptions should still be logged at error level
            LOG.error("Unexpected error during authentication", e);
            throw new RuntimeException("Authentication error: " + e.getMessage(), e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        try {
            // Remove "/realms/{realm}" from the end to get the base URL
            String keycloakBaseUrl = authServerUrl.substring(0, authServerUrl.lastIndexOf("/realms/"));
            
            // Extract realm name from the auth server URL
            String realm = authServerUrl.substring(authServerUrl.lastIndexOf("/") + 1);
            
            // Create Keycloak token endpoint URL
            String tokenEndpoint = keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
            
            // Build the REST client for Keycloak
            KeycloakTokenClient tokenClient = RestClientBuilder.newBuilder()
                    .baseUri(URI.create(tokenEndpoint))
                    .build(KeycloakTokenClient.class);
            
            // Prepare the form data for token refresh
            Form form = new Form()
                    .param("refresh_token", refreshTokenRequest.refreshToken())
                    .param("grant_type", "refresh_token")
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret);
            
            // Send the token refresh request to Keycloak
            Response response = tokenClient.getToken(form);
            
            if (response.getStatus() == 200) {
                Map<String, Object> tokenData = response.readEntity(Map.class);
                
                // Extract token information
                String accessToken = (String) tokenData.get("access_token");
                String refreshToken = (String) tokenData.get("refresh_token");
                String tokenType = (String) tokenData.get("token_type");
                int expiresIn = (Integer) tokenData.get("expires_in");
                
                // Extract roles from the access token
                List<String> roles = extractRolesFromToken(accessToken);
                
                return new TokenResponse(accessToken, refreshToken, tokenType, expiresIn, roles);
            } else {
                // This is an expected failure, log at debug level
                LOG.debug("Token refresh failed with status: " + response.getStatus());
                throw new NotAuthorizedException("Token refresh failed: " + response.getStatusInfo().getReasonPhrase());
            }
        } catch (WebApplicationException e) {
            // Specific web exceptions should be logged at debug level since they're expected
            LOG.debug("Token refresh failed: " + e.getMessage());
            throw new NotAuthorizedException("Token refresh failed: " + e.getMessage());
        } catch (Exception e) {
            // Unexpected exceptions should still be logged at error level
            LOG.error("Unexpected error during token refresh", e);
            throw new RuntimeException("Token refresh error: " + e.getMessage(), e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean logout(String refreshToken) {
        try {
            // Remove "/realms/{realm}" from the end to get the base URL
            String keycloakBaseUrl = authServerUrl.substring(0, authServerUrl.lastIndexOf("/realms/"));
            
            // Extract realm name from the auth server URL
            String realm = authServerUrl.substring(authServerUrl.lastIndexOf("/") + 1);
            
            // Create Keycloak logout endpoint URL
            String logoutEndpoint = keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
            
            // Build the REST client for Keycloak
            KeycloakLogoutClient logoutClient = RestClientBuilder.newBuilder()
                    .baseUri(URI.create(logoutEndpoint))
                    .build(KeycloakLogoutClient.class);
            
            // Prepare the form data for logout
            Form form = new Form()
                    .param("refresh_token", refreshToken)
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret);
            
            // Send the logout request to Keycloak
            Response response = logoutClient.logout(form);
            
            return response.getStatus() == 204 || response.getStatus() == 200;
        } catch (Exception e) {
            LOG.error("Error during logout", e);
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateToken(String token) {
        try {
            // Create a JWT consumer that doesn't verify signatures (Keycloak will do that)
            // This is just to validate the token structure and expiration
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setSkipSignatureVerification() // We rely on Keycloak for signature verification
                    .setSkipAllValidators() // Skip default validations since we're just parsing
                    .build();
            
            // Parse the token
            JwtClaims claims = jwtConsumer.processToClaims(token);
            
            // Check if token is expired
            // Get the expiration time
            NumericDate expirationTime = claims.getExpirationTime();
            
            // If there's no expiration claim, consider it invalid
            if (expirationTime == null) {
                return false;
            }
            
            // Check if the current time is after the expiration time
            NumericDate now = NumericDate.fromMilliseconds(System.currentTimeMillis());
            return now.isBefore(expirationTime);
        } catch (InvalidJwtException e) {
            // For expected validation failures, just log at debug level
            LOG.debug("Token validation failed: " + e.getMessage());
            return false;
        } catch (Exception e) {
            // For unexpected errors, log at error level
            LOG.error("Unexpected error during token validation", e);
            return false;
        }
    }
    
    /**
     * Extract roles from the JWT token
     * 
     * @param token The JWT token
     * @return List of role names
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRolesFromToken(String token) {
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setSkipSignatureVerification()
                    .setSkipAllValidators()
                    .build();
            
            JwtClaims claims = jwtConsumer.processToClaims(token);
            
            // Extract realm roles from "realm_access" claim
            Map<String, Object> realmAccess = claims.getClaimValue("realm_access", Map.class);
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                return roles;
            }
            
            // Extract groups if present (often used for roles)
            List<String> groups = claims.getStringListClaimValue("groups");
            if (groups != null) {
                return groups;
            }
            
            // Try to extract from scope if available
            String scope = claims.getStringClaimValue("scope");
            if (scope != null) {
                return Arrays.asList(scope.split(" "));
            }
            
            return Collections.emptyList();
        } catch (Exception e) {
            LOG.error("Error extracting roles from token", e);
            return Collections.emptyList();
        }
    }
}