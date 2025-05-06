package dev.tiodati.saas.gocommerce.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageServiceTest {

    private MessageService messageService;
    private TestLocaleResolver localeResolver;

    private final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    @BeforeEach
    void setUp() {
        // Using the default locale (English) from TestLocaleResolver
        localeResolver = new TestLocaleResolver();
        messageService = new MessageService();
        messageService.localeResolver = localeResolver;
    }

    @Test
    void getMessage_withValidKey_returnsMessage() {
        String result = messageService.getMessage("test.key");
        assertEquals("Test message", result);
    }

    @Test
    void getMessage_withValidKeyAndParams_returnsFormattedMessage() {
        String result = messageService.getMessage("test.key.params", "John", 5);
        assertEquals("Hello, John! You have 5 messages.", result);
    }

    @Test
    void getMessage_withMissingKey_returnsKeyItself() {
        String result = messageService.getMessage("missing.key");
        assertEquals("missing.key", result);
    }

    @Test
    void getMessage_withEmptyKey_returnsEmptyString() {
        String result = messageService.getMessage("");
        assertEquals("", result);
    }

    @Test
    void getMessage_withNullKey_returnsEmptyString() {
        String result = messageService.getMessage(null);
        assertEquals("", result);
    }

    @Test
    void getMessage_withSpecificLocale_returnsLocalizedMessage() {
        String result = messageService.getMessage("welcome", PT_BR);
        assertEquals("Bem vindo!", result);
    }

    @Test
    void getMessage_withSpecificLocaleAndParams_returnsFormattedLocalizedMessage() {
        String result = messageService.getMessage("greeting", PT_BR, "João", "Segunda-feira");
        assertEquals("Olá, João! Hoje é Segunda-feira.", result);
    }

    /**
     * Simple LocaleResolver implementation for testing
     */
    static class TestLocaleResolver implements LocaleResolver {
        
        /**
         * English is the default language for the application
         */
        public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
        
        private Locale locale;
        
        /**
         * Creates a TestLocaleResolver with the default locale (English)
         */
        public TestLocaleResolver() {
            this.locale = DEFAULT_LOCALE;
        }
        
        /**
         * Creates a TestLocaleResolver with a specific locale
         */
        public TestLocaleResolver(Locale locale) {
            this.locale = locale;
        }
        
        @Override
        public Locale getLocale() {
            return locale;
        }
        
        public void setLocale(Locale locale) {
            this.locale = locale;
        }

        @Override
        public void setLocale(String languageTag) {
            this.locale = Locale.forLanguageTag(languageTag);
        }
    }
}