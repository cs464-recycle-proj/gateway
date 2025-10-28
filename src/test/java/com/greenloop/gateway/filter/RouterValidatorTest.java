package com.greenloop.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RouterValidator}.
 * Tests endpoint security validation logic.
 */
class RouterValidatorTest {

    private RouterValidator routerValidator;

    @BeforeEach
    void setUp() {
        routerValidator = new RouterValidator();
    }

    @Test
    void testIsSecured_PublicEndpoint_Signup() {
        // Arrange
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/signup")
                .build();

        // Act
        boolean isSecured = routerValidator.isSecured.test(request);

        // Assert
        assertFalse(isSecured, "Signup endpoint should be public");
    }

    @Test
    void testIsSecured_PublicEndpoint_Login() {
        // Arrange
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login")
                .build();

        // Act
        boolean isSecured = routerValidator.isSecured.test(request);

        // Assert
        assertFalse(isSecured, "Login endpoint should be public");
    }

    @Test
    void testIsSecured_PublicEndpoint_Health() {
        // Arrange
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/health")
                .build();

        // Act
        boolean isSecured = routerValidator.isSecured.test(request);

        // Assert
        assertFalse(isSecured, "Health endpoint should be public");
    }

    @Test
    void testIsSecured_PublicEndpoint_CheckOTP() {
        // Arrange
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/verify/check-otp")
                .build();

        // Act
        boolean isSecured = routerValidator.isSecured.test(request);

        // Assert
        assertFalse(isSecured, "Check OTP endpoint should be public");
    }

    @Test
    void testIsSecured_PublicEndpoint_SendOTP() {
        // Arrange
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/verify/send-otp")
                .build();

        // Act
        boolean isSecured = routerValidator.isSecured.test(request);

        // Assert
        assertFalse(isSecured, "Send OTP endpoint should be public");
    }

    @Test
    void testIsSecured_SecuredEndpoint_UserProfile() {
        // Arrange
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile")
                .build();

        // Act
        boolean isSecured = routerValidator.isSecured.test(request);

        // Assert
        assertTrue(isSecured, "User profile endpoint should be secured");
    }

    @Test
    void testIsSecured_SecuredEndpoint_Events() {
        // Arrange
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/events")
                .build();

        // Act
        boolean isSecured = routerValidator.isSecured.test(request);

        // Assert
        assertTrue(isSecured, "Events endpoint should be secured");
    }

    @Test
    void testIsSecured_SecuredEndpoint_AuthLogout() {
        // Arrange
        ServerHttpRequest request = MockServerHttpRequest
                .post("/api/auth/logout")
                .build();

        // Act
        boolean isSecured = routerValidator.isSecured.test(request);

        // Assert
        assertTrue(isSecured, "Logout endpoint should be secured");
    }

    @Test
    void testIsSecured_SecuredEndpoint_SimilarPath() {
        // Arrange - path similar to public endpoint but not exact match
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/signup-confirm")
                .build();

        // Act
        boolean isSecured = routerValidator.isSecured.test(request);

        // Assert
        assertTrue(isSecured, "Similar but non-whitelisted path should be secured");
    }

    @Test
    void testOpenApiEndpoints_ContainsExpectedPaths() {
        // Assert
        assertEquals(5, RouterValidator.openApiEndpoints.size(), "Should have 5 public endpoints");
        assertTrue(RouterValidator.openApiEndpoints.contains("/api/auth/signup"));
        assertTrue(RouterValidator.openApiEndpoints.contains("/api/auth/login"));
        assertTrue(RouterValidator.openApiEndpoints.contains("/api/auth/health"));
        assertTrue(RouterValidator.openApiEndpoints.contains("/api/verify/check-otp"));
        assertTrue(RouterValidator.openApiEndpoints.contains("/api/verify/send-otp"));
    }
}
