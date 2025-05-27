package dev.tiodati.saas.gocommerce.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link MessageService} class.
 */
class MessageServiceTest {

    /**
     * The service under test.
     */
    private MessageService messageService;
    /**
     * A mock locale resolver for testing.
     */
    private TestLocaleResolver localeResolver;
    /**
     * The Brazilian Portuguese locale constant.
     */
    private static final Locale PT_BR_LOCALE = Locale.forLanguageTag("pt-BR");

    /**
     * Sets up the test environment before each test.
     */
    @BeforeEach
    void setUp() {
        // Using the default locale (English) from TestLocaleResolver
        localeResolver = new TestLocaleResolver();
        messageService = new MessageService();
        // Simulate injection or manual setting of the dependency
        messageService.localeResolver = localeResolver;
    }

    /**
     * Tests that getMessage returns the correct message for a valid key.
     */
    @Test
    void getMessageWithValidKeyReturnsMessage() {
        String result = messageService.getMessage("test.key");
        assertEquals("Test message", result);
    }

    /**
     * Tests that getMessage returns the correctly formatted message for a valid
     * key with parameters.
     */
    @Test
    void getMessageWithValidKeyAndParamsReturnsFormattedMessage() {
        String result = messageService.getMessage("test.key.params", "John", 5);
        assertEquals("Hello, John! You have 5 messages.", result);
    }

    /**
     * Tests that getMessage returns the key itself when the key is missing.
     */
    @Test
    void getMessageWithMissingKeyReturnsKeyItself() {
        String result = messageService.getMessage("missing.key");
        assertEquals("missing.key", result);
    }

    /**
     * Tests that getMessage returns an empty string when the key is empty.
     */
    @Test
    void getMessageWithEmptyKeyReturnsEmptyString() {
        String result = messageService.getMessage("");
        assertEquals("", result); // Added assertion to complete the test
    }

    /**
     * Tests that getMessage returns the localized message for a specific
     * locale.
     */
    @Test
    void getMessageWithSpecificLocaleReturnsLocalizedMessage() {
        // Ensure TestLocaleResolver is set to the specific locale for this test
        // if messageService.getMessage(key, locale) is not used
        // Or, if getMessage(key, locale) is intended, it should use that locale
        // directly.
        // Assuming messageService.getMessage(key, locale) uses the provided
        // locale argument:
        String result = messageService.getMessage("welcome", PT_BR_LOCALE);
        assertEquals("Bem vindo!", result);
    }

    /**
     * Tests that getMessage returns the formatted localized message for a
     * specific locale with parameters.
     */
    @Test
    void getMessageWithSpecificLocaleAndParamsReturnsFormattedLocalizedMessage() {
        String result = messageService.getMessage("greeting", PT_BR_LOCALE,
                "João", "Segunda-feira");
        assertEquals("Olá, João! Hoje é Segunda-feira.", result);
    }

    /**
     * Simple LocaleResolver implementation for testing purposes. This allows
     * controlling the locale returned during tests.
     */
    static class TestLocaleResolver implements LocaleResolver {

        /**
         * English is the default language for the application and tests.
         */
        public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

        /**
         * The current locale.
         */
        private Locale locale;

        /**
         * Creates a TestLocaleResolver with the default locale (English).
         */
        TestLocaleResolver() {
            this.locale = DEFAULT_LOCALE;
        }

        /**
         * Creates a TestLocaleResolver with a specific locale.
         *
         * @param initialLocale The initial locale to set.
         */
        TestLocaleResolver(Locale initialLocale) {
            this.locale = initialLocale;
        }

        @Override
        public Locale getLocale() {
            return locale;
        }

        /**
         * Sets the current locale for the resolver.
         *
         * @param newLocale The new locale to set.
         */
        @Override
        public void setLocale(Locale newLocale) {
            this.locale = newLocale;
        }

        /**
         * Sets the current locale for the resolver using a language tag.
         *
         * @param languageTag The language tag for the new locale.
         */
        @Override
        public void setLocale(String languageTag) {
            this.locale = Locale.forLanguageTag(languageTag);
        }
    }
}
