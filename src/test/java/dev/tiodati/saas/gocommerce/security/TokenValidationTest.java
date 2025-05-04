package dev.tiodati.saas.gocommerce.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Tests JWT token structure and validation using mocked tokens
 * instead of direct Keycloak integration.
 */
@QuarkusTest
public class TokenValidationTest {

    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    private static final String TEST_ISSUER = "http://localhost:9000/realms/gocommerce";
    
    @BeforeAll
    public static void generateKeys() throws Exception {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(2048);
        KeyPair kp = keyGenerator.generateKeyPair();
        privateKey = kp.getPrivate();
        publicKey = kp.getPublic();
    }

    /**
     * Tests that JWT tokens have expected standard claims
     */
    @Test
    public void testStandardClaims() throws Exception {
        // Create a test JWT token with standard claims
        JwtClaimsBuilder claims = Jwt.claims();
        claims.issuer(TEST_ISSUER);
        claims.upn("admin@gocommerce.dev");
        claims.subject("admin-user-id");
        claims.groups(new HashSet<>(Arrays.asList("admin", "user")));
        claims.claim(Claims.nickname.name(), "Admin User");
        
        // Sign with our test private key
        String token = claims.sign(privateKey);
                
        // Parse and validate the token
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts");
        
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Map<String, Object> parsedClaims = jsonb.fromJson(payload, Map.class);
            
            // Verify standard JWT claims
            assertNotNull(parsedClaims.get("sub"), "Subject claim should be present");
            assertEquals("admin-user-id", parsedClaims.get("sub"));
            assertNotNull(parsedClaims.get("iss"), "Issuer claim should be present");
            assertEquals(TEST_ISSUER, parsedClaims.get("iss"));
            assertNotNull(parsedClaims.get("iat"), "Issued at claim should be present");
        } catch (Exception e) {
            fail("Failed to parse token claims: " + e.getMessage());
        }
    }
    
    /**
     * Tests that JWT tokens include role information
     */
    @Test
    public void testRoleClaims() throws Exception {
        // Create roles and groups
        Set<String> roles = new HashSet<>(Arrays.asList("admin", "tenant-admin"));
        
        // Create a test JWT token with role claims
        JwtClaimsBuilder claims = Jwt.claims();
        claims.issuer(TEST_ISSUER);
        claims.upn("admin@gocommerce.dev");
        claims.subject("admin-user-id");
        claims.groups(roles);
        
        // Sign with our test private key
        String token = claims.sign(privateKey);
        
        // Parse and validate the token
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Map<String, Object> parsedClaims = jsonb.fromJson(payload, Map.class);
            
            // Verify groups/roles are present
            assertNotNull(parsedClaims.get("groups"), "Roles should be present");
            assertTrue(((java.util.List<?>)parsedClaims.get("groups")).contains("admin"), 
                    "Admin role should be present");
        } catch (Exception e) {
            fail("Failed to parse token claims: " + e.getMessage());
        }
    }
    
    /**
     * Tests that JWT tokens have proper expiration
     */
    @Test
    public void testTokenExpiration() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        long expiration = now + 3600; // 1 hour expiration
        
        JwtClaimsBuilder claims = Jwt.claims();
        claims.issuer(TEST_ISSUER);
        claims.subject("admin-user-id");
        claims.issuedAt(now);
        claims.expiresAt(expiration);
        
        // Sign with our test private key
        String token = claims.sign(privateKey);
        
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Map<String, Object> parsedClaims = jsonb.fromJson(payload, Map.class);
            
            // Verify expiration claim
            Double exp = ((Number)parsedClaims.get("exp")).doubleValue();
            Double iat = ((Number)parsedClaims.get("iat")).doubleValue();
            assertTrue(exp > iat, "Expiration should be after issuance");
            
            // Check it's about an hour difference
            double difference = exp - iat;
            assertEquals(3600, difference, 10); // Allow small tolerance
        } catch (Exception e) {
            fail("Failed to parse token claims: " + e.getMessage());
        }
    }
}