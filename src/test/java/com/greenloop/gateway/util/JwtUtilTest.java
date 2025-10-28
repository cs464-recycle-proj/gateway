package com.greenloop.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtUtil}.
 * Tests JWT token validation, parsing, and error handling.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String testSecret;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // Generate a test secret key (256 bits for HS256)
        secretKey = Jwts.SIG.HS256.key().build();
        testSecret = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        // Inject the test secret using reflection
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", testSecret);
    }

    @Test
    void testExtractAllClaims_ValidToken() {
        // Arrange
        String token = createTestToken("user123", "USER", "test@example.com", 3600000); // 1 hour

        // Act
        Claims claims = jwtUtil.extractAllClaims(token);

        // Assert
        assertNotNull(claims);
        assertEquals("user123", claims.getSubject());
        assertEquals("USER", claims.get("role"));
        assertEquals("test@example.com", claims.get("email"));
    }

    @Test
    void testExtractAllClaims_NullToken() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtUtil.extractAllClaims(null));
        assertEquals("Token cannot be null or empty", exception.getMessage());
    }

    @Test
    void testExtractAllClaims_EmptyToken() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtUtil.extractAllClaims(""));
        assertEquals("Token cannot be null or empty", exception.getMessage());
    }

    @Test
    void testExtractAllClaims_BlankToken() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtUtil.extractAllClaims("   "));
        assertEquals("Token cannot be null or empty", exception.getMessage());
    }

    @Test
    void testValidateToken_ValidToken() {
        // Arrange
        String token = createTestToken("user123", "USER", "test@example.com", 3600000);

        // Act & Assert
        assertDoesNotThrow(() -> jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_ExpiredToken() {
        // Arrange - create expired token (expired 1 hour ago)
        String token = createTestToken("user123", "USER", "test@example.com", -3600000);

        // Act & Assert
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_InvalidSignature() {
        // Arrange - create token with different secret
        SecretKey wrongKey = Jwts.SIG.HS256.key().build();
        String token = Jwts.builder()
                .subject("user123")
                .claim("role", "USER")
                .claim("email", "test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        // Act & Assert
        assertThrows(SignatureException.class, () -> jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_NullToken() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtUtil.validateToken(null));
        assertEquals("Token cannot be null or empty", exception.getMessage());
    }

    @Test
    void testValidateToken_MalformedToken() {
        // Arrange
        String malformedToken = "this.is.not.a.valid.jwt.token";

        // Act & Assert
        assertThrows(Exception.class, () -> jwtUtil.validateToken(malformedToken));
    }

    /**
     * Helper method to create a test JWT token.
     */
    private String createTestToken(String userId, String role, String email, long expirationOffset) {
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationOffset))
                .signWith(secretKey)
                .compact();
    }
}
