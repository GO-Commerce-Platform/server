package dev.tiodati.saas.gocommerce.auth.resource;

import dev.tiodati.saas.gocommerce.auth.dto.LoginRequest;
import dev.tiodati.saas.gocommerce.auth.dto.RefreshTokenRequest;
import dev.tiodati.saas.gocommerce.auth.dto.TokenResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthResourceTest {

    /**
     * Stores the access token obtained from a successful login or token
     * refresh. This token is used for testing protected endpoints that require
     * authentication.
     */
    private static String accessToken;

    /**
     * Stores the refresh token obtained from a successful login or token
     * refresh. Used for testing token refresh and logout functionalities.
     */
    private static String refreshToken;

    @Test
    @Order(1)
    void testLoginSuccess() {
        LoginRequest loginRequest = new LoginRequest("platform-admin",
                "platform-admin");

        TokenResponse tokenResponse = given().contentType(ContentType.JSON)
                .body(loginRequest).when().post("/api/auth/login").then()
                .statusCode(200).body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("tokenType", is("Bearer"))
                .body("expiresIn", notNullValue()).body("roles", notNullValue())
                .extract().as(TokenResponse.class);

        accessToken = tokenResponse.accessToken();
        refreshToken = tokenResponse.refreshToken();
    }

    @Test
    @Order(2)
    void testLoginFailureInvalidCredentials() {
        LoginRequest loginRequest = new LoginRequest("invaliduser",
                "wrongpassword");

        given().contentType(ContentType.JSON).body(loginRequest).when()
                .post("/api/auth/login").then().statusCode(401)
                .body("error", is(
                        "Authentication failed: Invalid username or password"));
    }

    @Test
    @Order(3)
    void testLoginFailureMissingUsername() {
        LoginRequest loginRequest = new LoginRequest(null, "admin");

        given().contentType(ContentType.JSON).body(loginRequest).when()
                .post("/api/auth/login").then().statusCode(400)
                .body("violations", notNullValue()); // Adjust to match actual
                                                     // error response for
                                                     // validation
    }

    @Test
    @Order(4)
    void testLoginFailureMissingPassword() {
        LoginRequest loginRequest = new LoginRequest("admin", "");

        given().contentType(ContentType.JSON).body(loginRequest).when()
                .post("/api/auth/login").then().statusCode(400)
                .body("violations", notNullValue()); // Adjust to match actual
                                                     // error response for
                                                     // validation
    }

    @Test
    @Order(5)
    void testRefreshTokenSuccess() {
        // Ensure this test runs after a successful login to have a valid
        // refreshToken
        if (refreshToken == null) {
            System.out.println(
                    "Skipping testRefreshTokenSuccess as refreshToken is null (previous login might have failed or not run)");
            return; // Or throw an assumption failed exception
        }
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(
                refreshToken);

        TokenResponse tokenResponse = given().contentType(ContentType.JSON)
                .body(refreshRequest).when().post("/api/auth/refresh").then()
                .statusCode(200).body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("tokenType", is("Bearer"))
                .body("expiresIn", notNullValue()).extract()
                .as(TokenResponse.class);

        accessToken = tokenResponse.accessToken(); // Update access token
        refreshToken = tokenResponse.refreshToken(); // Update refresh token
    }

    @Test
    @Order(6)
    void testRefreshTokenFailureInvalidToken() {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(
                "invalid-refresh-token");

        given().contentType(ContentType.JSON).body(refreshRequest).when()
                .post("/api/auth/refresh").then().statusCode(401)
                .body("error", is(
                        "Token refresh failed: Invalid or expired refresh token"));
    }

    @Test
    @Order(7)
    void testValidateTokenSuccess() {
        // Ensure this test runs after a successful login/refresh to have a
        // valid
        // accessToken
        if (accessToken == null) {
            System.out.println(
                    "Skipping testValidateTokenSuccess as accessToken is null (previous login/refresh might have failed or not run)");
            return; // Or throw an assumption failed exception
        }
        given().queryParam("token", accessToken).when()
                .get("/api/auth/validate").then().statusCode(200)
                .body("message", is("Token is valid"));
    }

    @Test
    @Order(8)
    void testValidateTokenFailureInvalidToken() {
        given().queryParam("token", "invalid-access-token").when()
                .get("/api/auth/validate").then().statusCode(401)
                .body("error", is("Token is invalid or expired"));
    }

    @Test
    @Order(9)
    void testValidateTokenFailureMissingToken() {
        given().when().get("/api/auth/validate").then().statusCode(400)
                .body("error", is("Token is required"));
    }

    @Test
    @Order(10)
    void testLogoutSuccess() {
        // Ensure this test runs after a successful login/refresh to have a
        // valid
        // refreshToken
        if (refreshToken == null) {
            System.out.println(
                    "Skipping testLogoutSuccess as refreshToken is null (previous login/refresh might have failed or not run)");
            return; // Or throw an assumption failed exception
        }
        given().queryParam("refreshToken", refreshToken).when()
                .delete("/api/auth/logout").then().statusCode(200)
                .body("message", is("Successfully logged out"));
    }

    @Test
    @Order(11)
    void testLogoutFailureInvalidToken() {
        given().queryParam("refreshToken",
                "completely-invalid-refresh-token-format").when()
                .delete("/api/auth/logout").then().statusCode(500)
                .body("error", notNullValue());
    }
}
