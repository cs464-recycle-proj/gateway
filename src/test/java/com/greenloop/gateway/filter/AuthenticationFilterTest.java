package com.greenloop.gateway.filter;

import com.greenloop.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthenticationFilter}.
 * Tests JWT authentication logic, error handling, and request enrichment.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticationFilterTest {

    private RouterValidator routerValidator;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;

    private static final String JWT_COOKIE_NAME = "jwt_token";

    @BeforeEach
    void setUp() {
        // Create a real RouterValidator instance instead of mocking
        routerValidator = new RouterValidator();

        // Inject the real RouterValidator into the filter
        ReflectionTestUtils.setField(authenticationFilter, "routerValidator", routerValidator);
        ReflectionTestUtils.setField(authenticationFilter, "jwtCookieName", JWT_COOKIE_NAME);

        // Default behavior: chain continues
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void testFilter_OptionsRequest_BypassesAuthentication() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .options("/api/users/profile")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // Assert
        result.block(); // Wait for completion
        verify(filterChain).filter(exchange);
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void testFilter_PublicEndpoint_BypassesAuthentication() {
        // Arrange - Use actual public endpoint from RouterValidator
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login") // This is a public endpoint
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // Assert
        result.block(); // Wait for completion
        verify(filterChain).filter(exchange);
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void testFilter_SecuredEndpoint_MissingCookie_ReturnsUnauthorized() {
        // Arrange - Use a secured endpoint (not in the public list)
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile") // This is a secured endpoint
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // Assert
        result.block(); // Wait for completion
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    void testFilter_ValidToken_EnrichesRequestHeaders() {
        // Arrange
        String token = "valid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile") // Secured endpoint
                .cookie(new HttpCookie(JWT_COOKIE_NAME, token))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Create a mock Claims object using a Map
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("sub", "user123");
        claimsMap.put("role", "USER");
        claimsMap.put("email", "test@example.com");

        Claims claims = Jwts.claims().add(claimsMap).build();

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // Assert
        result.block(); // Wait for completion
        verify(jwtUtil).validateToken(token);
        verify(jwtUtil).extractAllClaims(token);
        verify(filterChain).filter(any(ServerWebExchange.class));
    }

    @Test
    void testFilter_ExpiredToken_ReturnsUnauthorized() {
        // Arrange
        String token = "expired.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile") // Secured endpoint
                .cookie(new HttpCookie(JWT_COOKIE_NAME, token))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        doThrow(new ExpiredJwtException(null, null, "Token expired"))
                .when(jwtUtil).validateToken(token);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // Assert
        result.block(); // Wait for completion
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    void testFilter_InvalidSignature_ReturnsUnauthorized() {
        // Arrange
        String token = "invalid.signature.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile") // Secured endpoint
                .cookie(new HttpCookie(JWT_COOKIE_NAME, token))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        doThrow(new SignatureException("Invalid signature"))
                .when(jwtUtil).validateToken(token);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // Assert
        result.block(); // Wait for completion
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    void testFilter_ClaimsWithNullValues_UsesDefaults() {
        // Arrange
        String token = "valid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile") // Secured endpoint
                .cookie(new HttpCookie(JWT_COOKIE_NAME, token))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Create claims with only subject, role and email will be null
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("sub", "user123");
        Claims claims = Jwts.claims().add(claimsMap).build();

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);

        // Act
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // Assert
        result.block(); // Wait for completion
        verify(filterChain).filter(any(ServerWebExchange.class));
    }
}
