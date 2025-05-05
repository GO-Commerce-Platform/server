package dev.tiodati.saas.gocommerce.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.ext.web.RoutingContext;

@QuarkusTest
public class RequestLocaleResolverTest {

    @Mock
    CurrentVertxRequest currentRequest;
    
    @InjectMocks
    RequestLocaleResolver localeResolver = new RequestLocaleResolver();
    
    private RoutingContext routingContext;
    private HttpServerRequest httpRequest;
    
    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
        
        // Set up test doubles
        routingContext = mock(RoutingContext.class);
        httpRequest = mock(HttpServerRequest.class);
        
        when(currentRequest.getCurrent()).thenReturn(routingContext);
        when(routingContext.request()).thenReturn(httpRequest);
        
        // Default configuration
        localeResolver.defaultLocale = "en";
        localeResolver.supportedLocales = Optional.of("en,es,pt");
    }
    
    @Test
    void testGetLocale_defaultLocale_whenNoPreference() {
        // No locale preferences in request
        assertEquals(Locale.forLanguageTag("en"), localeResolver.getLocale());
    }
    
    @Test
    void testGetLocale_fromUrlParameter() {
        when(httpRequest.getParam("lang")).thenReturn("es");
        
        assertEquals(Locale.forLanguageTag("es"), localeResolver.getLocale());
    }
    
    @Test
    void testGetLocale_fromCookie() {
        Cookie cookie = new CookieImpl("locale", "pt");
        when(httpRequest.getCookie("locale")).thenReturn(cookie);
        
        assertEquals(Locale.forLanguageTag("pt"), localeResolver.getLocale());
    }
    
    @Test
    void testGetLocale_fromAcceptLanguageHeader() {
        when(httpRequest.getHeader("Accept-Language")).thenReturn("es-ES,es;q=0.9,en;q=0.8");
        
        assertEquals(Locale.forLanguageTag("es-ES"), localeResolver.getLocale());
    }
    
    @Test
    void testGetLocale_fallbackToDefault_whenUnsupportedLocale() {
        when(httpRequest.getParam("lang")).thenReturn("fr"); // Unsupported locale
        
        assertEquals(Locale.forLanguageTag("en"), localeResolver.getLocale());
    }
    
    @Test
    void testGetLocale_prioritizesUrlParameter_overCookie() {
        // Both URL parameter and cookie are present
        when(httpRequest.getParam("lang")).thenReturn("es");
        Cookie cookie = new CookieImpl("locale", "pt");
        when(httpRequest.getCookie("locale")).thenReturn(cookie);
        
        // Should use URL parameter
        assertEquals(Locale.forLanguageTag("es"), localeResolver.getLocale());
    }
    
    @Test
    void testGetLocale_prioritizesCookie_overAcceptLanguage() {
        // Both cookie and Accept-Language header are present
        when(httpRequest.getHeader("Accept-Language")).thenReturn("es-ES,es;q=0.9,en;q=0.8");
        Cookie cookie = new CookieImpl("locale", "pt");
        when(httpRequest.getCookie("locale")).thenReturn(cookie);
        
        // Should use cookie
        assertEquals(Locale.forLanguageTag("pt"), localeResolver.getLocale());
    }
}