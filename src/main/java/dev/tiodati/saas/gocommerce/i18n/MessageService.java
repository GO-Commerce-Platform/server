package dev.tiodati.saas.gocommerce.i18n;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Service for managing internationalized messages.
 * Provides methods to retrieve messages from resource bundles based on the current locale.
 */
@ApplicationScoped
public class MessageService {
    
    private static final String BUNDLE_NAME = "messages";
    
    @Inject
    @dev.tiodati.saas.gocommerce.i18n.Default
    LocaleResolver localeResolver;
    
    /**
     * Gets a message for the current locale.
     * 
     * @param key The message key
     * @return The localized message or the key itself if not found
     */
    public String getMessage(String key) {
        return getMessage(key, localeResolver.getLocale());
    }
    
    /**
     * Gets a message for the current locale with parameter substitution.
     * 
     * @param key The message key
     * @param params The parameters to substitute in the message
     * @return The formatted localized message or the key itself if not found
     */
    public String getMessage(String key, Object... params) {
        String message = getMessage(key);
        if (message.equals(key)) {
            return key;
        }
        return MessageFormat.format(message, params);
    }
    
    /**
     * Gets a message for a specific locale.
     * 
     * @param key The message key
     * @param locale The locale to use
     * @return The localized message or the key itself if not found
     */
    public String getMessage(String key, Locale locale) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            Log.warn("Message key not found: " + key + " for locale: " + locale);
            return key;
        } catch (Exception e) {
            Log.error("Error retrieving message: " + key, e);
            return key;
        }
    }
    
    /**
     * Gets a message for a specific locale with parameter substitution.
     * 
     * @param key The message key
     * @param locale The locale to use
     * @param params The parameters to substitute in the message
     * @return The formatted localized message or the key itself if not found
     */
    public String getMessage(String key, Locale locale, Object... params) {
        String message = getMessage(key, locale);
        if (message.equals(key)) {
            return key;
        }
        return MessageFormat.format(message, params);
    }
}