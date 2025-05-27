package dev.tiodati.saas.gocommerce.i18n.util;

import dev.tiodati.saas.gocommerce.i18n.LocaleResolver;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * A JAX-RS resource specifically for testing locale resolution mechanisms. It
 * uses the application's {@link LocaleResolver} to determine or set locales
 * based on simulated requests.
 */
@Path("/_test/locale") // Using a distinct path for test-only endpoints
public class TestLocaleResource {

    /**
     * The {@link LocaleResolver} instance used to resolve and set
     * locales for the current request. This is typically injected by the
     * application framework and is responsible for managing locale resolution
     * based on user preferences, request headers, or other criteria.
     */
    @Inject
    private LocaleResolver localeResolver;

    /**
     * Gets the locale resolved by {@link LocaleResolver} for the current
     * request.
     *
     * @return The language tag of the resolved locale.
     */
    @GET
    @Path("/resolve")
    @Produces(MediaType.TEXT_PLAIN)
    public String getResolvedLocale() {
        return localeResolver.getLocale().toLanguageTag();
    }

    /**
     * Sets the locale using {@link LocaleResolver} based on the provided
     * language tag. This typically results in setting a cookie.
     *
     * @param lang The language tag (e.g., "es", "pt-BR").
     * @return A No Content response if successful.
     */
    @PUT
    @Path("/set/{lang}")
    public Response setTestLocale(@PathParam("lang") String lang) {
        localeResolver.setLocale(lang);
        return Response.noContent().build();
    }
}
