package dev.tiodati.saas.gocommerce.auth.service;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST client interface for Keycloak token operations
 */
@RegisterRestClient
public interface KeycloakTokenClient {
    
    /**
     * Get a token from Keycloak
     * 
     * @param form Form containing grant type and credentials
     * @return Response with token data
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response getToken(Form form);
}