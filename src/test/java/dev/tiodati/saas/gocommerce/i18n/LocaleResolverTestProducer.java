package dev.tiodati.saas.gocommerce.i18n;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.Locale;

/**
 * Test producer for LocaleResolver to be used in tests.
 * This ensures that tests can run without requiring the full HTTP context.
 */
@ApplicationScoped
public class LocaleResolverTestProducer {

    /**
     * Produces a test implementation of the LocaleResolver for use in tests.
     * 
     * @return A simple LocaleResolver that returns a fixed locale
     */
    @Produces
    @Singleton
    @Alternative
    @Default
    public LocaleResolver produceTestLocaleResolver() {
        return new LocaleResolver() {
            private Locale currentLocale = Locale.ENGLISH;
            
            @Override
            public Locale getLocale() {
                return currentLocale;
            }
            
            @Override
            public void setLocale(Locale locale) {
                if (locale != null) {
                    this.currentLocale = locale;
                }
            }
            
            @Override
            public void setLocale(String languageTag) {
                if (languageTag != null && !languageTag.isEmpty()) {
                    this.currentLocale = Locale.forLanguageTag(languageTag);
                }
            }
        };
    }
}