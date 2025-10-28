package com.greenloop.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * Utility class for JWT (JSON Web Token) operations.
 * 
 * <p>
 * This class provides functionality for:
 * <ul>
 * <li>Parsing and validating JWT tokens</li>
 * <li>Extracting claims (user information) from tokens</li>
 * <li>Verifying token signatures using a secret key</li>
 * </ul>
 * 
 * <p>
 * The JWT secret is Base64-encoded and injected from application properties.
 * All tokens are validated using HMAC-SHA algorithms for signature
 * verification.
 * 
 * <p>
 * <b>Security Considerations:</b>
 * <ul>
 * <li>The secret key must be kept secure and never exposed</li>
 * <li>Tokens are verified for signature integrity and expiration</li>
 * <li>Invalid or expired tokens will throw exceptions</li>
 * </ul>
 * 
 * @author GreenLoop Team
 * @version 1.0
 * @see io.jsonwebtoken.Jwts
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Generates the HMAC signing key from the Base64-encoded secret.
     * 
     * <p>
     * This method decodes the JWT secret and creates a SecretKey
     * suitable for HMAC-SHA signature operations.
     * 
     * @return the SecretKey for JWT signature verification
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extracts all claims from a JWT token.
     * 
     * <p>
     * Parses the token and returns the payload containing user information
     * such as user ID (subject), role, email, and other custom claims.
     * 
     * @param token the JWT token string to parse (must not be null or empty)
     * @return the Claims object containing all token claims
     * @throws IllegalArgumentException     if token is null or empty
     * @throws io.jsonwebtoken.JwtException if token is invalid or expired
     */
    public Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates a JWT token's signature and expiration.
     * 
     * <p>
     * This method verifies that:
     * <ul>
     * <li>The token signature is valid</li>
     * <li>The token has not expired</li>
     * <li>The token format is correct</li>
     * </ul>
     * 
     * @param token the JWT token string to validate (must not be null or empty)
     * @throws IllegalArgumentException                    if token is null or empty
     * @throws io.jsonwebtoken.ExpiredJwtException         if token has expired
     * @throws io.jsonwebtoken.security.SignatureException if signature is invalid
     * @throws io.jsonwebtoken.JwtException                for other validation
     *                                                     failures
     */
    public void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
    }
}
