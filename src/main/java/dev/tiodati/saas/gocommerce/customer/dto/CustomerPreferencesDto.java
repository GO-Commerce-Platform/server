package dev.tiodati.saas.gocommerce.customer.dto;

import java.util.Map;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for customer preferences and settings.
 * Covers communication preferences, privacy settings, and other customizations.
 *
 * @param marketingEmailsOptIn        Whether customer opts in to marketing
 *                                    emails
 * @param promotionalSmsOptIn         Whether customer opts in to promotional
 *                                    SMS
 * @param orderUpdatesOptIn           Whether customer opts in to order update
 *                                    notifications
 * @param productRecommendationsOptIn Whether customer opts in to product
 *                                    recommendations
 * @param preferredLanguage           Customer's preferred language (ISO 639-1)
 * @param preferredCurrency           Customer's preferred currency (ISO 4217)
 * @param timezone                    Customer's preferred timezone
 * @param newsletterSubscription      Whether customer is subscribed to
 *                                    newsletter
 * @param privacyLevel                Customer's privacy level preference
 * @param communicationFrequency      How often customer wants to receive
 *                                    communications
 * @param customPreferences           Additional custom preferences as key-value
 *                                    pairs
 */
public record CustomerPreferencesDto(
        Boolean marketingEmailsOptIn,

        Boolean promotionalSmsOptIn,

        Boolean orderUpdatesOptIn,

        Boolean productRecommendationsOptIn,

        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language must be in ISO 639-1 format (e.g., 'en' or 'en-US')") String preferredLanguage,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be in ISO 4217 format (e.g., 'USD')") String preferredCurrency,

        @Size(max = 50, message = "Timezone cannot exceed 50 characters") String timezone,

        Boolean newsletterSubscription,

        PrivacyLevel privacyLevel,

        CommunicationFrequency communicationFrequency,

        Map<String, String> customPreferences) {

    /**
     * Enumeration for privacy levels.
     */
    public enum PrivacyLevel {
        /** Minimal data collection and sharing. */
        HIGH,
        /** Balanced privacy and personalization. */
        MEDIUM,
        /** Full personalization and data usage. */
        LOW
    }

    /**
     * Enumeration for communication frequency preferences.
     */
    public enum CommunicationFrequency {
        /** Daily communications. */
        DAILY,
        /** Weekly communications. */
        WEEKLY,
        /** Monthly communications. */
        MONTHLY,
        /** Only essential communications. */
        MINIMAL,
        /** No communications except critical updates. */
        NONE
    }

    /**
     * Creates default customer preferences.
     */
    public static CustomerPreferencesDto defaults() {
        return new CustomerPreferencesDto(
                false, // marketingEmailsOptIn
                false, // promotionalSmsOptIn
                true, // orderUpdatesOptIn
                false, // productRecommendationsOptIn
                "en", // preferredLanguage
                "USD", // preferredCurrency
                "UTC", // timezone
                false, // newsletterSubscription
                PrivacyLevel.MEDIUM, // privacyLevel
                CommunicationFrequency.WEEKLY, // communicationFrequency
                Map.of() // empty custom preferences
        );
    }

    /**
     * Creates privacy-focused preferences (minimal data sharing).
     */
    public static CustomerPreferencesDto privacyFocused() {
        return new CustomerPreferencesDto(
                false, // marketingEmailsOptIn
                false, // promotionalSmsOptIn
                true, // orderUpdatesOptIn (essential)
                false, // productRecommendationsOptIn
                "en", // preferredLanguage
                "USD", // preferredCurrency
                "UTC", // timezone
                false, // newsletterSubscription
                PrivacyLevel.HIGH, // privacyLevel
                CommunicationFrequency.MINIMAL, // communicationFrequency
                Map.of() // empty custom preferences
        );
    }

    /**
     * Creates marketing-friendly preferences (more data sharing).
     */
    public static CustomerPreferencesDto marketingFriendly() {
        return new CustomerPreferencesDto(
                true, // marketingEmailsOptIn
                true, // promotionalSmsOptIn
                true, // orderUpdatesOptIn
                true, // productRecommendationsOptIn
                "en", // preferredLanguage
                "USD", // preferredCurrency
                "UTC", // timezone
                true, // newsletterSubscription
                PrivacyLevel.LOW, // privacyLevel
                CommunicationFrequency.WEEKLY, // communicationFrequency
                Map.of() // empty custom preferences
        );
    }

    /**
     * Returns a copy with updated marketing preferences.
     */
    public CustomerPreferencesDto withMarketingPreferences(
            Boolean marketingEmails,
            Boolean promotionalSms,
            Boolean newsletter) {
        return new CustomerPreferencesDto(
                marketingEmails,
                promotionalSms,
                orderUpdatesOptIn,
                productRecommendationsOptIn,
                preferredLanguage,
                preferredCurrency,
                timezone,
                newsletter,
                privacyLevel,
                communicationFrequency,
                customPreferences);
    }

    /**
     * Returns a copy with updated localization preferences.
     */
    public CustomerPreferencesDto withLocalization(
            String language,
            String currency,
            String tz) {
        return new CustomerPreferencesDto(
                marketingEmailsOptIn,
                promotionalSmsOptIn,
                orderUpdatesOptIn,
                productRecommendationsOptIn,
                language,
                currency,
                tz,
                newsletterSubscription,
                privacyLevel,
                communicationFrequency,
                customPreferences);
    }

    /**
     * Returns a copy with updated privacy settings.
     */
    public CustomerPreferencesDto withPrivacySettings(
            PrivacyLevel level,
            CommunicationFrequency frequency) {
        return new CustomerPreferencesDto(
                marketingEmailsOptIn,
                promotionalSmsOptIn,
                orderUpdatesOptIn,
                productRecommendationsOptIn,
                preferredLanguage,
                preferredCurrency,
                timezone,
                newsletterSubscription,
                level,
                frequency,
                customPreferences);
    }

    /**
     * Returns a copy with additional custom preferences.
     */
    public CustomerPreferencesDto withCustomPreference(String key, String value) {
        var newCustomPreferences = customPreferences != null
                ? Map.<String, String>copyOf(customPreferences)
                : Map.<String, String>of();

        var updatedPreferences = new java.util.HashMap<>(newCustomPreferences);
        updatedPreferences.put(key, value);

        return new CustomerPreferencesDto(
                marketingEmailsOptIn,
                promotionalSmsOptIn,
                orderUpdatesOptIn,
                productRecommendationsOptIn,
                preferredLanguage,
                preferredCurrency,
                timezone,
                newsletterSubscription,
                privacyLevel,
                communicationFrequency,
                Map.copyOf(updatedPreferences));
    }

    /**
     * Checks if customer has opted in to any marketing communications.
     */
    public boolean hasMarketingOptIns() {
        return Boolean.TRUE.equals(marketingEmailsOptIn) ||
                Boolean.TRUE.equals(promotionalSmsOptIn) ||
                Boolean.TRUE.equals(newsletterSubscription);
    }

    /**
     * Checks if customer prefers minimal communications.
     */
    public boolean prefersMinimalCommunication() {
        return communicationFrequency == CommunicationFrequency.MINIMAL ||
                communicationFrequency == CommunicationFrequency.NONE;
    }

    /**
     * Gets the effective language, defaulting to English if not set.
     */
    public String getEffectiveLanguage() {
        return preferredLanguage != null && !preferredLanguage.trim().isEmpty()
                ? preferredLanguage
                : "en";
    }

    /**
     * Gets the effective currency, defaulting to USD if not set.
     */
    public String getEffectiveCurrency() {
        return preferredCurrency != null && !preferredCurrency.trim().isEmpty()
                ? preferredCurrency
                : "USD";
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
