package dev.tiodati.saas.gocommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents the request payload for validating a token.
 *
 * @param token The token string to be validated.
 */
public record TokenValidationRequest(
    @NotBlank(message = "Token must not be blank")
    String token
) {
}
