package dev.tiodati.saas.gocommerce.auth.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * CDI producer for Keycloak admin client.
 * Creates and configures a Keycloak admin client bean using application
 * properties.
 */
@ApplicationScoped
public class KeycloakProducer {

    @ConfigProperty(name = "quarkus.keycloak.admin-client.server-url")
    String serverUrl;

    @ConfigProperty(name = "quarkus.keycloak.admin-client.realm")
    String realm;

    @ConfigProperty(name = "quarkus.keycloak.admin-client.client-id")
    String clientId;

    @ConfigProperty(name = "quarkus.keycloak.admin-client.username")
    String username;

    @ConfigProperty(name = "quarkus.keycloak.admin-client.password")
    String password;

    /**
     * Produces a Keycloak admin client instance.
     * Uses username/password authentication for the admin client.
     *
     * @return Configured Keycloak admin client
     */
    @Produces
    @Singleton
    public Keycloak produceKeycloakAdminClient() {
        Log.infof("Creating Keycloak admin client for server: %s, realm: %s", serverUrl, realm);

        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .username(username)
                .password(password)
                .build();
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
