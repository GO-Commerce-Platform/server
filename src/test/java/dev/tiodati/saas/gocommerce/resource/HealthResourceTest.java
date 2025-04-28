package dev.tiodati.saas.gocommerce.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HealthResourceTest {

    @Test
    public void testHealthEndpoint() { // Renamed test method for clarity
        given()
          .when().get("/health") // Corrected path to /health
          .then()
             .statusCode(200)
             .body("status", is("UP")); // Basic check for UP status
    }

}