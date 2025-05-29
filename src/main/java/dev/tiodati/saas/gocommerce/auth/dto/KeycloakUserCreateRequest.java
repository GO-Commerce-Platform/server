package dev.tiodati.saas.gocommerce.auth.dto;

// This is a simplified DTO. You might want to expand it based on Keycloak's UserRepresentation.
public class KeycloakUserCreateRequest {
    /**
     * The username.
     */
    private String username;
    /**
     * The email.
     */
    private String email;
    /**
     * The first name.
     */
    private String firstName;
    /**
     * The last name.
     */
    private String lastName;
    /**
     * The password.
     */
    private String password; // Handle securely
    /**
     * Whether the email is verified.
     */
    private boolean emailVerified = false; // Default, can be set to true if verified externally

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
