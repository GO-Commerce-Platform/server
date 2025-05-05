package dev.tiodati.saas.gocommerce.i18n;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Locale;
import java.util.Optional;

/**
 * Implementation of LocaleResolver that resolves the locale from the HTTP request.
 * Looks for locale information in the following order:
 * 1. URL parameter "lang"
 * 2. Cookie "locale"
 * 3. Accept-Language header
 * 4. Default locale from configuration
 */
@RequestScoped
public class RequestLocaleResolver implements LocaleResolver {
    
    private static final Logger LOG = Logger.getLogger(RequestLocaleResolver.class);
    private static final String LANG_PARAM = "lang";
    private static final String LOCALE_COOKIE = "locale";
    
    @Inject
    CurrentVertxRequest currentRequest;
    
    @ConfigProperty(name = "quarkus.default-locale", defaultValue = "en")
    String defaultLocale;
    
    @ConfigProperty(name = "quarkus.locales")
    Optional<String> supportedLocales;
    
    private Locale currentLocale;
    
    @Override
    public Locale getLocale() {
        if (currentLocale != null) {
            return currentLocale;
        }
        
        currentLocale = resolveLocale();
        return currentLocale;
    }
    
    @Override
    public void setLocale(Locale locale) {
        if (locale != null && isSupportedLocale(locale.toLanguageTag())) {
            currentLocale = locale;
            
            // Store in cookie for future requests
            if (currentRequest != null && currentRequest.getCurrent() != null) {
                currentRequest.getCurrent().response()
                    .addCookie(io.vertx.core.http.Cookie.cookie(LOCALE_COOKIE, locale.toLanguageTag())
                        .setPath("/")
                        .setMaxAge(365 * 24 * 60 * 60) // 1 year
                        .setSecure(true)
                        .setHttpOnly(true));
            }
        }
    }
    
    @Override
    public void setLocale(String languageTag) {
        if (languageTag != null && !languageTag.isEmpty()) {
            try {
                Locale locale = Locale.forLanguageTag(languageTag);
                setLocale(locale);
            } catch (Exception e) {
                LOG.warn("Invalid locale language tag: " + languageTag, e);
            }
        }
    }
    
    /**
     * Resolve the locale from the current request.
     */
    private Locale resolveLocale() {
        RoutingContext routingContext = currentRequest != null ? currentRequest.getCurrent() : null;
        if (routingContext == null) {
            return Locale.forLanguageTag(defaultLocale);
        }
        
        // First check URL parameter
        String langParam = routingContext.request().getParam(LANG_PARAM);
        if (langParam != null && !langParam.isEmpty() && isSupportedLocale(langParam)) {
            return Locale.forLanguageTag(langParam);
        }
        
        // Then check cookie
        io.vertx.core.http.Cookie localeCookie = routingContext.request().getCookie(LOCALE_COOKIE);
        if (localeCookie != null && isSupportedLocale(localeCookie.getValue())) {
            return Locale.forLanguageTag(localeCookie.getValue());
        }
        
        // Then check Accept-Language header
        String acceptLanguage = routingContext.request().getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            // Parse the Accept-Language header
            // Format is typically: "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5"
            String[] languages = acceptLanguage.split(",");
            for (String language : languages) {
                // Extract the language tag (without quality factor)
                String languageTag = language.trim().split(";")[0].trim();
                if (isSupportedLocale(languageTag)) {
                    return Locale.forLanguageTag(languageTag);
                }
            }
        }
        
        // Default locale as fallback
        return Locale.forLanguageTag(defaultLocale);
    }
    
    /**
     * Check if the given locale is supported by the application.
     * 
     * @param localeTag The locale language tag to check
     * @return true if supported, false otherwise
     */
    private boolean isSupportedLocale(String localeTag) {
        if (supportedLocales.isEmpty()) {
            // If no supported locales are configured, accept any locale
            return true;
        }
        
        String[] locales = supportedLocales.get().split(",");
        for (String supportedLocale : locales) {
            // Simple match for language part (e.g., "en" matches "en-US")
            if (localeTag.trim().toLowerCase().startsWith(supportedLocale.trim().toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
}