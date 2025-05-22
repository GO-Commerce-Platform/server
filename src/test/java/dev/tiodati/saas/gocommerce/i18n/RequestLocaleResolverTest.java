package dev.tiodati.saas.gocommerce.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

@QuarkusTest
public class RequestLocaleResolverTest {

    private RequestLocaleResolver localeResolver;
    private RoutingContext routingContext;
    private HttpServerRequest request;
    private HttpServerResponse response;

    @BeforeEach
    public void setUp() {
        localeResolver = new RequestLocaleResolver();
        routingContext = mock(RoutingContext.class);
        request = mock(HttpServerRequest.class);
        response = mock(HttpServerResponse.class);

        when(routingContext.request()).thenReturn(request);
        when(routingContext.response()).thenReturn(response);

        // Correctly mock and assign CurrentVertxRequest
        CurrentVertxRequest mockCvRequest = mock(CurrentVertxRequest.class);
        when(mockCvRequest.getCurrent()).thenReturn(routingContext);
        localeResolver.currentRequest = mockCvRequest; // Assign the mock object

        localeResolver.defaultLocale = "en";
        localeResolver.supportedLocales = Optional.of("en,es,pt");
    }

    @Test
    public void testResolveLocaleFromUrlParameter() {
        when(request.getParam("lang")).thenReturn("es");

        Locale locale = localeResolver.getLocale();

        assertNotNull(locale);
        assertEquals("es", locale.getLanguage());
    }

    @Test
    public void testResolveLocaleFromCookie() {
        when(request.getParam("lang")).thenReturn(null);
        Cookie cookie = Cookie.cookie("locale", "pt");
        when(request.getCookie("locale")).thenReturn(cookie);

        Locale locale = localeResolver.getLocale();

        assertNotNull(locale);
        assertEquals("pt", locale.getLanguage());
    }

    @Test
    public void testResolveLocaleFromAcceptLanguageHeader() {
        when(request.getParam("lang")).thenReturn(null);
        when(request.getCookie("locale")).thenReturn(null);
        when(request.getHeader("Accept-Language")).thenReturn("fr-CH,fr;q=0.9,en;q=0.8");

        Locale locale = localeResolver.getLocale();

        assertNotNull(locale);
        assertEquals("en", locale.getLanguage());
    }

    @Test
    public void testDefaultLocaleFallback() {
        when(request.getParam("lang")).thenReturn(null);
        when(request.getCookie("locale")).thenReturn(null);
        when(request.getHeader("Accept-Language")).thenReturn(null);

        Locale locale = localeResolver.getLocale();

        assertNotNull(locale);
        assertEquals("en", locale.getLanguage());
    }

    @Test
    public void testSetLocale() {
        localeResolver.setLocale("es");

        Locale locale = localeResolver.getLocale();

        assertNotNull(locale);
        assertEquals("es", locale.getLanguage());
        verify(response).addCookie(Mockito.argThat(cookie ->
            cookie.getName().equals("locale") &&
            cookie.getValue().equals("es")
        ));
    }
}