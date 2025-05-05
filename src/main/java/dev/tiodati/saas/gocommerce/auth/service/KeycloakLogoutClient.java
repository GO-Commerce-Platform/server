package dev.tiodati.saas.gocommerce.auth.service;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST client interface for Keycloak logout operations
 */
@RegisterRestClient
public interface KeycloakLogoutClient {
    
    /**
     * Logout from Keycloak
     * 
     * @param form Form containing refresh token and client info
     * @return Response with logout status
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response logout(Form form);
}