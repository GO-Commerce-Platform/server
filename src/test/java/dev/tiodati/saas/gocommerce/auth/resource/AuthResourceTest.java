package dev.tiodati.saas.gocommerce.auth.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import dev.tiodati.saas.gocommerce.auth.dto.LoginRequest;
import dev.tiodati.saas.gocommerce.auth.dto.RefreshTokenRequest;
import dev.tiodati.saas.gocommerce.auth.dto.TokenResponse;
import dev.tiodati.saas.gocommerce.auth.service.AuthService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.NotAuthorizedException;

@QuarkusTest
public class AuthResourceTest {

    @InjectMock
    AuthService authService;
    
    @Test
    public void testLoginSuccess() {
        // Mock data
        TokenResponse mockResponse = new TokenResponse(
            "mock-access-token",
            "mock-refresh-token",
            "Bearer",
            3600,
            Arrays.asList("admin", "user")
        );
        
        // Mock service
        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);
        
        // Test request
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\":\"admin\",\"password\":\"password123\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .body("accessToken", is("mock-access-token"))
            .body("refreshToken", is("mock-refresh-token"))
            .body("tokenType", is("Bearer"))
            .body("expiresIn", is(3600))
            .body("roles", notNullValue())
            .body("roles.size()", is(2))
            .body("roles[0]", is("admin"))
            .body("roles[1]", is("user"));
    }
    
    @Test
    public void testLoginFailure() {
        // Mock service failure
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new NotAuthorizedException("Invalid credentials"));
        
        // Test request
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\":\"baduser\",\"password\":\"wrongpassword\"}")
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401)
            .body("error", notNullValue());
    }
    
    @Test
    public void testRefreshTokenSuccess() {
        // Mock data
        TokenResponse mockResponse = new TokenResponse(
            "new-access-token",
            "new-refresh-token",
            "Bearer",
            3600,
            Arrays.asList("admin", "user")
        );
        
        // Mock service
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(mockResponse);
        
        // Test request
        given()
            .contentType(ContentType.JSON)
            .body("{\"refreshToken\":\"valid-refresh-token\"}")
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(200)
            .body("accessToken", is("new-access-token"))
            .body("refreshToken", is("new-refresh-token"));
    }
    
    @Test
    public void testRefreshTokenFailure() {
        // Mock service failure
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
            .thenThrow(new NotAuthorizedException("Invalid refresh token"));
        
        // Test request
        given()
            .contentType(ContentType.JSON)
            .body("{\"refreshToken\":\"invalid-refresh-token\"}")
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(401)
            .body("error", notNullValue());
    }
    
    @Test
    public void testLogoutSuccess() {
        // Mock service
        when(authService.logout("valid-refresh-token")).thenReturn(true);
        
        // Test request
        given()
            .queryParam("refreshToken", "valid-refresh-token")
        .when()
            .delete("/api/auth/logout")
        .then()
            .statusCode(200)
            .body("message", equalTo("Successfully logged out"));
    }
    
    @Test
    public void testLogoutFailure() {
        // Mock service
        when(authService.logout("invalid-refresh-token")).thenReturn(false);
        
        // Test request
        given()
            .queryParam("refreshToken", "invalid-refresh-token")
        .when()
            .delete("/api/auth/logout")
        .then()
            .statusCode(500)
            .body("error", equalTo("Logout failed"));
    }
    
    @Test
    public void testValidateTokenSuccess() {
        // Mock service
        when(authService.validateToken("valid-token")).thenReturn(true);
        
        // Test request
        given()
            .queryParam("token", "valid-token")
        .when()
            .get("/api/auth/validate")
        .then()
            .statusCode(200)
            .body("message", equalTo("Token is valid"));
    }
    
    @Test
    public void testValidateTokenFailure() {
        // Mock service
        when(authService.validateToken("invalid-token")).thenReturn(false);
        
        // Test request
        given()
            .queryParam("token", "invalid-token")
        .when()
            .get("/api/auth/validate")
        .then()
            .statusCode(401)
            .body("error", equalTo("Token is invalid or expired"));
    }
}