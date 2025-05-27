package dev.tiodati.saas.gocommerce.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Utility class for password hashing and verification.
 *
 * Note: In a production environment, consider using a more robust library
 * like Argon2, BCrypt, or PBKDF2.
 */
@ApplicationScoped
public class PasswordHashingUtil {

    private static final int SALT_LENGTH = 16;
    private static final String ALGORITHM = "SHA-512";
    private static final String SEPARATOR = ":";

    /**
     * Hash a password with a generated sal
     *
     * @param password The password to hash
     * @return A string in format "salt:hashedPassword"
     */
    public String hashPassword(String password) {
        try {
            // Generate a random sal
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // Hash password with sal
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // Convert both to Base64 for storage
            String saltString = Base64.getEncoder().encodeToString(salt);
            String hashedPasswordString = Base64.getEncoder().encodeToString(hashedPassword);

            // Return combined salt:hash
            return saltString + SEPARATOR + hashedPasswordString;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Verify if a password matches a stored hash
     *
     * @param password The password to verify
     * @param storedHash The stored hash (salt:hash)
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(String password, String storedHash) {
        try {
            // Split stored hash into salt and hash
            String[] parts = storedHash.split(SEPARATOR);
            if (parts.length != 2) {
                return false;
            }

            // Decode salt and hash
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHashBytes = Base64.getDecoder().decode(parts[1]);

            // Hash password with stored sal
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // Compare hashes
            if (hashedPassword.length != storedHashBytes.length) {
                return false;
            }

            // Time-constant comparison to prevent timing attacks
            int diff = 0;
            for (int i = 0; i < hashedPassword.length; i++) {
                diff |= hashedPassword[i] ^ storedHashBytes[i];
            }
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
