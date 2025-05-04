package dev.tiodati.saas.gocommerce.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class PasswordHashingUtilTest {
    
    @Inject
    PasswordHashingUtil passwordHashingUtil;
    
    @Test
    void testHashPassword() {
        // Arrange
        String password = "securePassword123";
        
        // Act
        String hashedPassword = passwordHashingUtil.hashPassword(password);
        
        // Assert
        assertNotNull(hashedPassword);
        assertNotEquals(password, hashedPassword);
        assertTrue(hashedPassword.contains(":"), "Hashed password should contain separator");
        
        String[] parts = hashedPassword.split(":");
        assertEquals(2, parts.length, "Hashed password should have two parts (salt and hash)");
    }
    
    @Test
    void testVerifyPassword_correctPassword() {
        // Arrange
        String password = "securePassword123";
        String hashedPassword = passwordHashingUtil.hashPassword(password);
        
        // Act
        boolean result = passwordHashingUtil.verifyPassword(password, hashedPassword);
        
        // Assert
        assertTrue(result, "Password verification should succeed with correct password");
    }
    
    @Test
    void testVerifyPassword_incorrectPassword() {
        // Arrange
        String password = "securePassword123";
        String wrongPassword = "wrongPassword456";
        String hashedPassword = passwordHashingUtil.hashPassword(password);
        
        // Act
        boolean result = passwordHashingUtil.verifyPassword(wrongPassword, hashedPassword);
        
        // Assert
        assertFalse(result, "Password verification should fail with wrong password");
    }
    
    @Test
    void testVerifyPassword_invalidHash() {
        // Arrange
        String password = "securePassword123";
        String invalidHash = "invalidHash";
        
        // Act
        boolean result = passwordHashingUtil.verifyPassword(password, invalidHash);
        
        // Assert
        assertFalse(result, "Password verification should fail with invalid hash format");
    }
}