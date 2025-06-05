package dev.tiodati.saas.gocommerce.auth.service;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import dev.tiodati.saas.gocommerce.auth.dto.TokenResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;

/**
 * REST client interface for interacting with Keycloak's OpenID Connect token
 * endpoint. This client is used to obtain and refresh access tokens. The base
 * URL for this client is typically configured via MicroProfile Config, pointing
 * to the Keycloak realm's base URL (e.g.,
 * http://keycloak-host/realms/your-realm).
 */
@RegisterRestClient(configKey = "keycloak-token-client")
// The base URI for this client will be derived from
// quarkus.oidc.auth-server-url
// or explicitly set via mp-rest/url property for this client interface.
// The @Path annotation here is relative to that base URI.
@Path("/protocol/openid-connect/token")
public interface KeycloakTokenClient {

    /**
     * Exchanges an authorization grant (like password or refresh token) for an
     * access token. This method corresponds to Keycloak's token endpoint.
     *
     * @param form A JAX-RS {@link Form} containing the grant type, client
     *             credentials, and other necessary parameters (e.g.,
     *             username/password or refresh_token).
     * @return A {@link TokenResponse} containing the access token, refresh
     *         token, and other token metadata.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    TokenResponse grantToken(Form form);
}
