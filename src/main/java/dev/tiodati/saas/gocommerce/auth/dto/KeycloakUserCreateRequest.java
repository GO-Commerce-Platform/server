package dev.tiodati.saas.gocommerce.auth.dto;

/**
 * Request DTO for creating a Keycloak user.
 * This is a simplified DTO based on Keycloak's UserRepresentation.
 *
 * @param username      The username
 * @param email         The user's email address
 * @param firstName     The user's first name
 * @param lastName      The user's last name
 * @param password      The user's password (handle securely)
 * @param emailVerified Whether the email is verified
 */
public record KeycloakUserCreateRequest(
        String username,
        String email,
        String firstName,
        String lastName,
        String password,
        boolean emailVerified) {
    /**
     * Factory method for creating a new user with unverified email.
     */
    public static KeycloakUserCreateRequest newUser(
            String username,
            String email,
            String firstName,
            String lastName,
            String password) {
        return new KeycloakUserCreateRequest(
                username,
                email,
                firstName,
                lastName,
                password,
                false // emailVerified defaults to false
        );
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
