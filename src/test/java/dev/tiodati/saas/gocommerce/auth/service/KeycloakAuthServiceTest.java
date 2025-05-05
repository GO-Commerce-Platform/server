package dev.tiodati.saas.gocommerce.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.tiodati.saas.gocommerce.auth.dto.LoginRequest;
import dev.tiodati.saas.gocommerce.auth.dto.RefreshTokenRequest;
import dev.tiodati.saas.gocommerce.auth.dto.TokenResponse;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthServiceTest {

    @Mock
    private KeycloakTokenClient tokenClient;
    
    @Mock
    private KeycloakLogoutClient logoutClient;
    
    @Mock
    private RestClientBuilder restClientBuilder;
    
    @InjectMocks
    private KeycloakAuthService authService;
    
    private static final String TEST_SECRET = "testsecretkeythatisveryverylongandshouldbemorethan256bits";
    private static final byte[] TEST_SECRET_BYTES = TEST_SECRET.getBytes();
    
    private String generateMockToken(String subject, List<String> roles, long expiration) throws JoseException {
        JwtClaims claims = new JwtClaims();
        claims.setSubject(subject);
        claims.setExpirationTimeMinutesInTheFuture(30);
        claims.setIssuedAtToNow();
        
        // Add roles in the format Keycloak uses
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", roles);
        claims.setClaim("realm_access", realmAccess);
        
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(new HmacKey(TEST_SECRET_BYTES));
        jws.setAlgorithmHeaderValue("HS256");
        
        return jws.getCompactSerialization();
    }
    
    @BeforeEach
    void setUp() throws Exception {
        // Set up service properties
        authService.authServerUrl = "http://localhost:9000/realms/gocommerce";
        authService.clientId = "gocommerce-client";
        authService.clientSecret = "testsecret";
        
        // Use lenient() for mocks that may not be used in all tests to avoid UnnecessaryStubbingException
        lenient().when(restClientBuilder.baseUri(any(URI.class))).thenReturn(restClientBuilder);
        lenient().when(restClientBuilder.build(KeycloakTokenClient.class)).thenReturn(tokenClient);
        lenient().when(restClientBuilder.build(KeycloakLogoutClient.class)).thenReturn(logoutClient);
    }
    
    @Test
    void testLogin_Success() throws Exception {
        // Mock token response data
        String accessToken = generateMockToken("testuser", Arrays.asList("user", "admin"), 30);
        String refreshToken = "test-refresh-token";
        String tokenType = "Bearer";
        int expiresIn = 300;
        
        // Create mock response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("access_token", accessToken);
        responseData.put("refresh_token", refreshToken);
        responseData.put("token_type", tokenType);
        responseData.put("expires_in", expiresIn);
        
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockResponse.readEntity(Map.class)).thenReturn(responseData);
        
        // Mock token client
        when(tokenClient.getToken(any(Form.class))).thenReturn(mockResponse);
        
        // Execute with mocked RestClientBuilder
        try (MockedStatic<RestClientBuilder> mockedStatic = Mockito.mockStatic(RestClientBuilder.class)) {
            mockedStatic.when(RestClientBuilder::newBuilder).thenReturn(restClientBuilder);
            
            // Test login
            LoginRequest request = new LoginRequest("testuser", "password123");
            TokenResponse response = authService.login(request);
            
            // Verify response
            assertNotNull(response);
            assertEquals(accessToken, response.accessToken());
            assertEquals(refreshToken, response.refreshToken());
            assertEquals(tokenType, response.tokenType());
            assertEquals(expiresIn, response.expiresIn());
            assertTrue(response.roles().contains("admin"));
            assertTrue(response.roles().contains("user"));
        }
    }
    
    @Test
    void testLogin_Failure() {
        // Create mock error response
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(401);
        when(mockResponse.getStatusInfo()).thenReturn(Response.Status.UNAUTHORIZED);
        
        // Mock token client
        when(tokenClient.getToken(any(Form.class))).thenReturn(mockResponse);
        
        // Execute with mocked RestClientBuilder
        try (MockedStatic<RestClientBuilder> mockedStatic = Mockito.mockStatic(RestClientBuilder.class)) {
            mockedStatic.when(RestClientBuilder::newBuilder).thenReturn(restClientBuilder);
            
            // Test login with invalid credentials
            LoginRequest request = new LoginRequest("testuser", "wrongpassword");
            
            // Verify exception is thrown
            assertThrows(NotAuthorizedException.class, () -> authService.login(request));
        }
    }
    
    @Test
    void testRefreshToken_Success() throws Exception {
        // Mock token response data
        String accessToken = generateMockToken("testuser", Arrays.asList("user"), 30);
        String refreshToken = "new-refresh-token";
        String tokenType = "Bearer";
        int expiresIn = 300;
        
        // Create mock response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("access_token", accessToken);
        responseData.put("refresh_token", refreshToken);
        responseData.put("token_type", tokenType);
        responseData.put("expires_in", expiresIn);
        
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockResponse.readEntity(Map.class)).thenReturn(responseData);
        
        // Mock token client
        when(tokenClient.getToken(any(Form.class))).thenReturn(mockResponse);
        
        // Execute with mocked RestClientBuilder
        try (MockedStatic<RestClientBuilder> mockedStatic = Mockito.mockStatic(RestClientBuilder.class)) {
            mockedStatic.when(RestClientBuilder::newBuilder).thenReturn(restClientBuilder);
            
            // Test refresh token
            RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
            TokenResponse response = authService.refreshToken(request);
            
            // Verify response
            assertNotNull(response);
            assertEquals(accessToken, response.accessToken());
            assertEquals(refreshToken, response.refreshToken());
            assertEquals(tokenType, response.tokenType());
            assertEquals(expiresIn, response.expiresIn());
            assertTrue(response.roles().contains("user"));
        }
    }
    
    @Test
    void testRefreshToken_Failure() {
        // Create mock error response
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(400);
        when(mockResponse.getStatusInfo()).thenReturn(Response.Status.BAD_REQUEST);
        
        // Mock token client
        when(tokenClient.getToken(any(Form.class))).thenReturn(mockResponse);
        
        // Execute with mocked RestClientBuilder
        try (MockedStatic<RestClientBuilder> mockedStatic = Mockito.mockStatic(RestClientBuilder.class)) {
            mockedStatic.when(RestClientBuilder::newBuilder).thenReturn(restClientBuilder);
            
            // Test refresh with invalid token
            RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");
            
            // Verify exception is thrown
            assertThrows(NotAuthorizedException.class, () -> authService.refreshToken(request));
        }
    }
    
    @Test
    void testLogout_Success() {
        // Create mock response
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(204);
        
        // Mock logout client
        when(logoutClient.logout(any(Form.class))).thenReturn(mockResponse);
        
        // Execute with mocked RestClientBuilder
        try (MockedStatic<RestClientBuilder> mockedStatic = Mockito.mockStatic(RestClientBuilder.class)) {
            mockedStatic.when(RestClientBuilder::newBuilder).thenReturn(restClientBuilder);
            
            // Test logout
            boolean result = authService.logout("valid-refresh-token");
            
            // Verify success
            assertTrue(result);
        }
    }
    
    @Test
    void testLogout_Failure() {
        // Create mock error response
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(400);
        
        // Mock logout client
        when(logoutClient.logout(any(Form.class))).thenReturn(mockResponse);
        
        // Execute with mocked RestClientBuilder
        try (MockedStatic<RestClientBuilder> mockedStatic = Mockito.mockStatic(RestClientBuilder.class)) {
            mockedStatic.when(RestClientBuilder::newBuilder).thenReturn(restClientBuilder);
            
            // Test logout with invalid token
            boolean result = authService.logout("invalid-refresh-token");
            
            // Verify failure
            assertFalse(result);
        }
    }
    
    @Test
    void testValidateToken_ValidToken() throws Exception {
        // Generate a valid token with future expiration
        String validToken = generateMockToken("testuser", Arrays.asList("user"), 30);
        
        // Test validation
        boolean result = authService.validateToken(validToken);
        
        // Verify success
        assertTrue(result);
    }
    
    @Test
    void testValidateToken_InvalidToken() {
        // Create a more realistic but invalid token format 
        // (three parts separated by dots, but with invalid content)
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString("{\"sub\":\"test\",\"exp\":0}".getBytes());
        String signature = "invalid-signature";
        String invalidToken = header + "." + payload + "." + signature;
        
        // Test validation
        boolean result = authService.validateToken(invalidToken);
        
        // Verify failure
        assertFalse(result);
    }
    
    @Test
    void testValidateToken_ExpiredToken() throws Exception {
        // Create JWT claims with past expiration
        JwtClaims claims = new JwtClaims();
        claims.setSubject("testuser");
        claims.setIssuedAtToNow();
        // Set expiration to 1 hour in the past
        claims.setExpirationTimeMinutesInTheFuture(-60);
        
        // Add roles
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Arrays.asList("user"));
        claims.setClaim("realm_access", realmAccess);
        
        // Generate token
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(new HmacKey(TEST_SECRET_BYTES));
        jws.setAlgorithmHeaderValue("HS256");
        String expiredToken = jws.getCompactSerialization();
        
        // Test validation
        boolean result = authService.validateToken(expiredToken);
        
        // Verify failure due to expiration
        assertFalse(result);
    }
}