package dev.tiodati.saas.gocommerce.auth.service;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST client interface for interacting with Keycloak's OpenID Connect logout
 * endpoint. This client is used to invalidate a user's session, typically by
 * revoking their refresh token. The base URL for this client is typically
 * configured via MicroProfile Config, pointing to the Keycloak realm's base URL
 * (e.g., http://keycloak-host/realms/your-realm).
 */
@RegisterRestClient
// The base URI for this client will be the auth-server-url (e.g.,
// http://localhost:9000/realms/gocommerce)
// The @Path annotation here is relative to that base URI.
@Path("/protocol/openid-connect/logout")
public interface KeycloakLogoutClient {

    /**
     * Logs out a user by invalidating their session/token. This typically
     * involves sending client credentials and the refresh token to Keycloak.
     *
     * @param form A JAX-RS {@link Form} containing parameters like client_id,
     *             client_secret, and refresh_token.
     * @return A JAX-RS {@link Response}. Successful logout usually returns HTTP
     *         204 No Content.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response logout(Form form);
}
