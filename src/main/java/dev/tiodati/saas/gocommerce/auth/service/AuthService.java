package dev.tiodati.saas.gocommerce.auth.service;

import dev.tiodati.saas.gocommerce.auth.dto.LoginRequest;
import dev.tiodati.saas.gocommerce.auth.dto.RefreshTokenRequest;
import dev.tiodati.saas.gocommerce.auth.dto.TokenResponse;

/**
 * Service interface for authentication operations
 */
public interface AuthService {
    
    /**
     * Authenticate a user and generate access and refresh tokens
     *
     * @param loginRequest Contains username and password
     * @return TokenResponse with access and refresh tokens
     * @throws jakarta.ws.rs.NotAuthorizedException if authentication fails
     */
    TokenResponse login(LoginRequest loginRequest);
    
    /**
     * Generate new tokens using a refresh token
     *
     * @param refreshTokenRequest Contains the refresh token
     * @return TokenResponse with new access and refresh tokens
     * @throws jakarta.ws.rs.NotAuthorizedException if refresh token is invalid or expired
     */
    TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
    
    /**
     * Invalidate a user's refresh token
     *
     * @param refreshToken The token to invalidate
     * @return true if successfully invalidated
     */
    boolean logout(String refreshToken);
    
    /**
     * Validate an access token
     * 
     * @param token The token to validate
     * @return true if token is valid
     */
    boolean validateToken(String token);
}