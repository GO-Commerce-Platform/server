package dev.tiodati.saas.gocommerce.auth.dto;

import java.util.List;

/**
 * DTO for token response containing access token, refresh token and other details
 */
public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    int expiresIn,
    List<String> roles
) { }
