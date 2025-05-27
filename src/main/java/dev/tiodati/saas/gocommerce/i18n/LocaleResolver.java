package dev.tiodati.saas.gocommerce.i18n;

import java.util.Locale;

/**
 * Interface for resolving the locale for the current request.
 * Implementations can use different strategies like request headers,
 * cookies, URL parameters, or user preferences.
 */
public interface LocaleResolver {

    /**
     * Get the locale for the current request.
     *
     * @return The resolved Locale objec
     */
    Locale getLocale();

    /**
     * Set the locale for the current request.
     *
     * @param locale The locale to se
     */
    void setLocale(Locale locale);

    /**
     * Set the locale using a language tag string.
     *
     * @param languageTag The language tag (e.g., "en", "es", "pt-BR")
     */
    void setLocale(String languageTag);
}
