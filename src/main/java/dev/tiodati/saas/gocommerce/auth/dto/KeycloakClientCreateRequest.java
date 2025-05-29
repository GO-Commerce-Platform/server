package dev.tiodati.saas.gocommerce.auth.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

// This is a simplified DTO. You might want to expand it based on Keycloak's ClientRepresentation.
@Data
public class KeycloakClientCreateRequest {
    /** The client ID. */
    private String clientId;
    /** The client name. */
    private String name;
    /** The client description. */
    private String description;
    /** The client secret. */
    private String secret; // Consider secure generation and handling
    /** Whether standard flow is enabled. */
    private boolean standardFlowEnabled = true;
    /** Whether direct access grants are enabled. */
    private boolean directAccessGrantsEnabled = true;
    /** Whether service accounts are enabled. */
    private boolean serviceAccountsEnabled = false;
    /** Whether the client is public. */
    private boolean publicClient = false;
    /** The list of redirect URIs. */
    private List<String> redirectUris = List.of();
    /** The list of web origins. */
    private List<String> webOrigins = List.of();
    /** Additional attributes for the client. */
    private Map<String, String> attributes; // For additional attributes like 'pkce.code.challenge.method'
}
