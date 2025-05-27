package dev.tiodati.saas.gocommerce.i18n;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Integration tests for {@link RequestLocaleResolver}. These tests verify
 * locale resolution by making HTTP requests to a test endpoint that utilizes
 * the {@link RequestLocaleResolver}.
 */
@QuarkusTest
public class RequestLocaleResolverTest { // Consider renaming to
                                         // RequestLocaleResolverIntegrationTest

    /**
     * Endpoint for testing locale resolution. This endpoint is used to
     * verify that the locale is correctly resolved based on different sources
     * such as URL parameters, cookies, and Accept-Language headers.
     */
    private static final String TEST_ENDPOINT_RESOLVE = "/_test/locale/resolve";

    /**
     * Endpoint for testing setting the locale via a URL parameter. This endpoint
     * allows setting the locale and expects the "locale" cookie to be set in the
     * response.
     */
    private static final String TEST_ENDPOINT_SET_LANG = "/_test/locale/set/{lang}";

    /**
     * Tests that the locale is correctly resolved from the "lang" URL
     * parameter. Assumes "es" is a supported locale.
     */
    @Test
    public void testResolveLocaleFromUrlParameter() {
        given().queryParam("lang", "es").when().get(TEST_ENDPOINT_RESOLVE)
                .then().statusCode(200).body(equalTo("es"));
    }

    /**
     * Tests that the locale is correctly resolved from the "locale" cookie.
     * Assumes "pt" is a supported locale or will be resolved (e.g. to pt-BR).
     * The RequestLocaleResolver logic might map "pt" to "pt-BR" if "pt-BR" is
     * configured and "pt" is a prefix. For this test, we expect the resolver to
     * handle "pt".
     */
    @Test
    public void testResolveLocaleFromCookie() {
        given().cookie("locale", "pt").when().get(TEST_ENDPOINT_RESOLVE).then()
                .statusCode(200).body(equalTo("pt")); // RequestLocaleResolver
                                                      // supports "pt" and
                                                      // "pt-BR"
    }

    /**
     * Tests that the locale is correctly resolved from the Accept-Language
     * header. Assumes "es" is preferred and supported.
     */
    @Test
    public void testResolveLocaleFromAcceptLanguageHeader() {
        given().header("Accept-Language", "es,en;q=0.8").when()
                .get(TEST_ENDPOINT_RESOLVE).then().statusCode(200)
                .body(equalTo("es"));
    }

    /**
     * Tests that the resolver falls back to the default locale ("en") when no
     * other source provides a locale.
     */
    @Test
    public void testDefaultLocaleFallback() {
        given()
                // No lang param, no cookie, no specific Accept-Language that
                // matches other than default
                .when().get(TEST_ENDPOINT_RESOLVE).then().statusCode(200)
                .body(equalTo("en")); // Default locale is 'en'
    }

    /**
     * Tests setting the locale via an endpoint and verifies that the "locale"
     * cookie is set correctly. Then, it makes a subsequent request to ensure
     * the new locale is resolved (implicitly via the cookie).
     */
    @Test
    public void testSetLocaleAndVerifyCookie() {
        // Set locale to "es"
        given().pathParam("lang", "es").when().put(TEST_ENDPOINT_SET_LANG)
                .then().statusCode(204) // No Content
                .cookie("locale", "es"); // Verify cookie is set in the response

        // Verify that a subsequent request (simulating browser sending the
        // cookie) resolves to "es"
        given().cookie("locale", "es") // Simulate the cookie being sent back by
                                       // the client
                .when().get(TEST_ENDPOINT_RESOLVE).then().statusCode(200)
                .body(equalTo("es"));
    }
}
