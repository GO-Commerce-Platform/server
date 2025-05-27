package dev.tiodati.saas.gocommerce.i18n;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Producer for LocaleResolver implementations.
 * Uses CDI to provide the appropriate LocaleResolver implementation.
 */
@ApplicationScoped
public class LocaleResolverProducer {

    @Inject
    RequestLocaleResolver requestLocaleResolver;

    /**
     * Produces a LocaleResolver implementation.
     * Currently returns the RequestLocaleResolver which uses HTTP request
     * information.
     *
     * @return A LocaleResolver implementation
     */
    @Produces
    @dev.tiodati.saas.gocommerce.i18n.Default
    public LocaleResolver produceLocaleResolver() {
        return requestLocaleResolver;
    }
}
