package dev.tiodati.saas.gocommerce.resource;

import dev.tiodati.saas.gocommerce.i18n.Default;
import dev.tiodati.saas.gocommerce.i18n.LocaleResolver;
import dev.tiodati.saas.gocommerce.i18n.MessageService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * REST resource for managing locale and demonstrating internationalization.
 */
@Path("/api/locale")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Internationalization", description = "Operations for managing localization and internationalization")
public class LocaleResource {
    
    @Inject
    @Default
    LocaleResolver localeResolver;
    
    @Inject
    MessageService messageService;
    
    @ConfigProperty(name = "quarkus.locales")
    Optional<String> supportedLocales;
    
    /**
     * Get information about current locale and some example internationalized messages.
     * 
     * @return Response containing locale information and sample messages
     */
    @GET
    @Operation(summary = "Get locale information", description = "Returns current locale information and example messages")
    public Response getLocaleInfo() {
        Locale currentLocale = localeResolver.getLocale();
        
        Map<String, Object> result = new HashMap<>();
        result.put("locale", currentLocale.toLanguageTag());
        result.put("language", currentLocale.getLanguage());
        result.put("displayLanguage", currentLocale.getDisplayLanguage());
        
        // Add some example messages
        Map<String, String> messages = new HashMap<>();
        messages.put("welcome", messageService.getMessage("common.success"));
        messages.put("productName", messageService.getMessage("product.name"));
        messages.put("orderStatus", messageService.getMessage("order.status"));
        messages.put("customerInfo", messageService.getMessage("customer.firstName") + " & " + 
                messageService.getMessage("customer.lastName"));
        
        // Add validation message with parameter
        messages.put("validation", messageService.getMessage("validation.minLength", 8));
        
        result.put("messages", messages);
        
        return Response.ok(result).build();
    }
    
    /**
     * Change the current locale.
     * 
     * @param localeTag The locale language tag to set (e.g. "en", "es", "pt")
     * @return Response indicating success and the new locale
     */
    @POST
    @Path("/{localeTag}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Operation(summary = "Change locale", description = "Updates the current locale based on the provided language tag")
    public Response changeLocale(@PathParam("localeTag") String localeTag) {
        if (localeTag == null || localeTag.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Locale tag is required"))
                    .build();
        }
        
        // Validate the locale tag against supported locales
        if (!isSupportedLocale(localeTag)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Unsupported locale: " + localeTag))
                    .build();
        }
        
        try {
            // Only set the locale if it's valid
            Locale newLocale = Locale.forLanguageTag(localeTag);
            localeResolver.setLocale(newLocale);
            
            Locale currentLocale = localeResolver.getLocale();
            Map<String, Object> result = new HashMap<>();
            result.put("locale", currentLocale.toLanguageTag());
            result.put("language", currentLocale.getLanguage());
            result.put("displayLanguage", currentLocale.getDisplayLanguage());
            result.put("message", messageService.getMessage("common.success"));
            
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid locale tag: " + localeTag))
                    .build();
        }
    }
    
    /**
     * Check if the given locale is supported by the application.
     * 
     * @param localeTag The locale language tag to check
     * @return true if supported, false otherwise
     */
    private boolean isSupportedLocale(String localeTag) {
        if (supportedLocales.isEmpty()) {
            // If no supported locales are defined, we only accept known valid locales
            try {
                Locale locale = Locale.forLanguageTag(localeTag);
                // Check if this is a reasonable locale (has at least a language)
                return locale != null && !locale.getLanguage().isEmpty();
            } catch (Exception e) {
                return false;
            }
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