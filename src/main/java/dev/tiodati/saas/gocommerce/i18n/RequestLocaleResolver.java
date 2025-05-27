package dev.tiodati.saas.gocommerce.i18n;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.logging.Log;

import java.util.Locale;
import java.util.Optional;

/**
 * Implementation of {@link LocaleResolver} that determines the user's locale
 * based on HTTP request information. The locale is resolved by checking the
 * following sources in order of priority:
 * <ol>
 * <li>URL query parameter named "lang" (e.g., /products?lang=es)</li>
 * <li>Cookie named "locale"</li>
 * <li>Accept-Language HTTP header sent by the client's browser</li>
 * <li>A default locale configured for the application (fallback)</li>
 * </ol>
 */
@RequestScoped
public class RequestLocaleResolver implements LocaleResolver {

    /**
     * Name of the URL query parameter used for specifying the language.
     */
    private static final String LANG_PARAM = "lang";
    /**
     * Name of the cookie used for storing the user's locale preference.
     */
    private static final String LOCALE_COOKIE = "locale";

    private final CurrentVertxRequest currentRequest;
    private final String defaultLocale;
    private final Optional<String> supportedLocales;

    /**
     * Caches the resolved locale for the current request to avoid redundant
     * lookups.
     */
    private Locale currentLocale;

    /**
     * Constructs a new RequestLocaleResolver.
     *
     * @param currentRequest   The current Vert.x HTTP request context.
     * @param defaultLocale    The default locale string.
     * @param supportedLocales Optional string of comma-separated supported
     *                         locales.
     */
    @Inject
    public RequestLocaleResolver(CurrentVertxRequest currentRequest,
            @ConfigProperty(name = "quarkus.default-locale", defaultValue = "en") String defaultLocale,
            @ConfigProperty(name = "quarkus.locales") Optional<String> supportedLocales) {
        this.currentRequest = currentRequest;
        this.defaultLocale = defaultLocale;
        this.supportedLocales = supportedLocales;
    }

    /**
     * Retrieves the resolved locale for the current request. If the locale has
     * already been resolved and cached, the cached value is returned.
     * Otherwise, it resolves the locale using {@link #resolveLocale()} and
     * caches it.
     *
     * @return The {@link Locale} for the current request.
     */
    @Override
    public Locale getLocale() {
        // Return cached locale if already resolved for this request
        if (this.currentLocale != null) {
            return this.currentLocale;
        }

        // Resolve and cache the locale
        this.currentLocale = resolveLocale();
        return this.currentLocale;
    }

    /**
     * Sets the locale for the current user and persists it in a cookie for
     * subsequent requests. The provided locale will only be set if it's a
     * supported locale as defined by {@link #isSupportedLocale(String)}.
     *
     * @param locale The {@link Locale} to set.
     */
    @Override
    public void setLocale(Locale locale) {
        if (locale != null && isSupportedLocale(locale.toLanguageTag())) {
            this.currentLocale = locale;

            // Store the selected locale in a cookie for future requests.
            // The cookie is set to be secure, HTTP-only, and expires in 1 year.
            if (this.currentRequest != null
                    && this.currentRequest.getCurrent() != null) {
                this.currentRequest.getCurrent().response()
                        .addCookie(io.vertx.core.http.Cookie
                                .cookie(LOCALE_COOKIE, locale.toLanguageTag())
                                .setPath("/") // Cookie accessible from all
                                              // paths
                                .setMaxAge(365 * 24 * 60 * 60) // Expires in 1
                                                               // year
                                .setSecure(true) // Transmit only over HTTPS
                                .setHttpOnly(true)); // Not accessible via
                                                     // client-side JavaScript
            }
        }
    }

    /**
     * Sets the locale for the current user using a language tag (e.g., "en-US")
     * and persists it. This method converts the language tag to a
     * {@link Locale} object and then calls {@link #setLocale(Locale)}. Logs a
     * warning if the provided language tag is invalid.
     *
     * @param languageTag The language tag string representing the locale to
     *                    set.
     */
    @Override
    public void setLocale(String languageTag) {
        if (languageTag != null && !languageTag.isEmpty()) {
            try {
                setLocale(Locale.forLanguageTag(languageTag));
            } catch (IllegalArgumentException e) { // More specific exception
                Log.warn("Invalid locale language tag: " + languageTag, e);
            }
        }
    }

    /**
     * Resolves the locale by inspecting the current HTTP request. It checks for
     * the locale in the URL parameter, cookie, and Accept-Language header, in
     * that order. If no locale is found in these sources, it falls back to the
     * default application locale.
     *
     * @return The resolved {@link Locale}.
     */
    private Locale resolveLocale() {
        RoutingContext routingContext = this.currentRequest != null
                ? this.currentRequest.getCurrent()
                : null;
        // If there's no active HTTP request, fall back to the default locale.
        if (routingContext == null) {
            Log.debug("No routing context available, using default locale");
            return Locale.forLanguageTag(this.defaultLocale);
        }

        // 1. Check for locale in the "lang" URL query parameter.
        String langParam = routingContext.request().getParam(LANG_PARAM);
        if (langParam != null && !langParam.isEmpty()
                && isSupportedLocale(langParam)) {
            return Locale.forLanguageTag(langParam);
        }

        // 2. Check for locale in the "locale" cookie.
        io.vertx.core.http.Cookie localeCookie = routingContext.request()
                .getCookie(LOCALE_COOKIE);
        if (localeCookie != null
                && isSupportedLocale(localeCookie.getValue())) {
            return Locale.forLanguageTag(localeCookie.getValue());
        }

        // 3. Check for locale in the "Accept-Language" HTTP header.
        String acceptLanguageHeader = routingContext.request()
                .getHeader("Accept-Language");
        if (acceptLanguageHeader != null && !acceptLanguageHeader.isEmpty()) {
            // Parse the Accept-Language header to find the best matching
            // supported locale.
            // This can be complex due to quality values (e.g.,
            // "en-US,en;q=0.9,es;q=0.8").
            // For simplicity, we take the first language tag if supported.
            // A more robust solution would involve parsing and matching quality
            // values.
            String[] preferredLocales = acceptLanguageHeader.split(",")[0]
                    .split(";")[0].trim().split("-");
            String lang = preferredLocales[0];
            if (isSupportedLocale(lang)) {
                return Locale.forLanguageTag(lang);
            }
        }

        // 4. If no locale is found from other sources, use the application's
        // default
        // locale.
        Log.debug("Using default application locale: " + this.defaultLocale);
        return Locale.forLanguageTag(this.defaultLocale);
    }

    /**
     * Checks if a given locale (represented by its language tag) is supported
     * by the application. Support is determined by the 'quarkus.locales'
     * configuration property. If 'quarkus.locales' is not configured, all
     * locales are considered supported.
     *
     * @param localeTag The language tag of the locale to check (e.g., "en",
     *                  "pt-BR").
     * @return {@code true} if the locale is supported or if no specific locales
     *         are configured; {@code false} otherwise.
     */
    private boolean isSupportedLocale(String localeTag) {
        // If the 'quarkus.locales' property is not set, consider all locales as
        // supported.
        if (this.supportedLocales.isEmpty()) {
            return true;
        }

        // Get the comma-separated list of supported locales from configuration.
        String[] localesArray = this.supportedLocales.get().split(",");
        for (String supportedLocale : localesArray) {
            // Perform a case-insensitive check. Allows for partial matches
            // (e.g., "en"
            // matches "en-US").
            if (localeTag.trim().toLowerCase()
                    .startsWith(supportedLocale.trim().toLowerCase())) {
                return true;
            }
        }

        // The provided localeTag is not in the list of supported locales.
        return false;
    }
}
