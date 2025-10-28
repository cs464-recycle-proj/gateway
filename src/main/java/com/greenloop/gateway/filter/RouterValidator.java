package com.greenloop.gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Validator that determines which API endpoints require authentication.
 * 
 * <p>
 * This component maintains a whitelist of public endpoints that can be
 * accessed without JWT authentication. All other endpoints are considered
 * secured and require valid authentication tokens.
 * 
 * <p>
 * <b>Public Endpoints (No Authentication Required):</b>
 * <ul>
 * <li>/api/auth/signup - User registration</li>
 * <li>/api/auth/login - User authentication</li>
 * <li>/api/auth/health - Service health check</li>
 * <li>/api/verify/check-otp - OTP verification</li>
 * <li>/api/verify/send-otp - OTP generation and sending</li>
 * </ul>
 * 
 * <p>
 * All other endpoints require authentication and will be validated by
 * the {@link AuthenticationFilter}.
 * 
 * @author GreenLoop Team
 * @version 1.0
 * @see AuthenticationFilter
 */
@Component
public class RouterValidator {

    /**
     * List of API endpoints that are publicly accessible without authentication.
     * These endpoints bypass the JWT validation filter.
     */
    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/health",
            "/api/verify/check-otp",
            "/api/verify/send-otp");

    /**
     * Predicate that tests whether a request requires authentication.
     * 
     * <p>
     * Returns true if the request path is NOT in the whitelist,
     * meaning it requires authentication. Returns false for public endpoints.
     * 
     * @see #openApiEndpoints
     */
    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();
        return openApiEndpoints.stream().noneMatch(path::equals);
    };
}
