package dev.tiodati.saas.gocommerce.auth.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for token response containing access token, refresh token and other
 * details.
 *
 * @param accessToken  The JWT access token
 * @param refreshToken The refresh token for token renewal
 * @param tokenType    The type of token (typically "Bearer")
 * @param expiresIn    Token expiration time in seconds
 * @param roles        List of user roles (populated from JWT claims)
 */
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,

        @JsonProperty("refresh_token") String refreshToken,

        @JsonProperty("token_type") String tokenType,

        @JsonProperty("expires_in") int expiresIn,

        // Note: roles are typically contained within the JWT itself, not in the token
        // response
        // This field might be populated by parsing the JWT claims after token exchange
        List<String> roles) {
}
