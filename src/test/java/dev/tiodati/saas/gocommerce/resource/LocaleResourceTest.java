package dev.tiodati.saas.gocommerce.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class LocaleResourceTest {
    
    @Test
    public void testGetLocaleInfo() {
        given()
            .when()
                .get("/api/locale")
            .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("locale", notNullValue())
                .body("language", notNullValue())
                .body("displayLanguage", notNullValue())
                .body("messages", notNullValue())
                .body("messages.productName", notNullValue());
    }
    
    @Test
    public void testChangeLocale_toSpanish() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .when()
                .post("/api/locale/es")
            .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("locale", is("es"))
                .body("language", is("es"))
                .body("displayLanguage", notNullValue())
                .body("message", is("Éxito")); // Success in Spanish
    }
    
    @Test
    public void testChangeLocale_toPortuguese() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .when()
                .post("/api/locale/pt")
            .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("locale", is("pt"))
                .body("language", is("pt"))
                .body("displayLanguage", notNullValue())
                .body("message", is("Sucesso")); // Success in Portuguese
    }
    
    @Test
    public void testChangeLocale_withInvalidLocale() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .when()
                .post("/api/locale/invalid")
            .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body("error", notNullValue());
    }
}