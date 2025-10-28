package com.greenloop.gateway.filter;

import com.greenloop.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpMethod;

import java.nio.charset.StandardCharsets;

/**
 * Gateway filter that authenticates incoming requests using JWT tokens from
 * cookies.
 * 
 * <p>
 * This filter implements the authentication layer for the API gateway by:
 * <ul>
 * <li>Extracting JWT tokens from HTTP cookies</li>
 * <li>Validating token signatures and expiration</li>
 * <li>Extracting user claims (ID, role, email) from valid tokens</li>
 * <li>Enriching downstream requests with user information headers</li>
 * <li>Bypassing authentication for whitelisted public endpoints</li>
 * </ul>
 * 
 * <p>
 * The filter adds the following headers to authenticated requests:
 * <ul>
 * <li><b>X-User-ID</b>: The unique identifier of the authenticated user</li>
 * <li><b>X-User-Role</b>: The role of the user (e.g., USER, ADMIN)</li>
 * <li><b>X-User-Email</b>: The email address of the user</li>
 * </ul>
 * 
 * <p>
 * These headers enable downstream microservices to access user information
 * without needing to validate JWT tokens themselves, centralizing
 * authentication logic.
 * 
 * @author GreenLoop Team
 * @version 1.0
 * @see JwtUtil
 * @see RouterValidator
 */
@RefreshScope
@Component
@Slf4j
public class AuthenticationFilter implements GatewayFilter {

    @Autowired
    private RouterValidator routerValidator;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.cookie.name}")
    private String jwtCookieName;

    /**
     * Filters incoming requests to validate JWT authentication.
     * 
     * <p>
     * This method implements the main authentication flow:
     * <ol>
     * <li>Bypasses OPTIONS requests for CORS preflight</li>
     * <li>Checks if the endpoint requires authentication</li>
     * <li>Extracts and validates JWT token from cookies</li>
     * <li>Extracts user claims and enriches request headers</li>
     * <li>Forwards authenticated request to downstream services</li>
     * </ol>
     * 
     * @param exchange the current server exchange containing request and response
     * @param chain    the filter chain for processing the request
     * @return a Mono that completes when the request processing is done
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            log.info("OPTIONS request bypassing authentication filter.");
            return chain.filter(exchange); // Pass it immediately to the next filter (CorsFilter)
        }

        if (routerValidator.isSecured.test(request)) {
            HttpCookie authCookie = request.getCookies().getFirst(jwtCookieName);

            if (authCookie == null) {
                log.warn("Missing authorization cookie for path: {}", request.getURI().getPath());
                return this.onError(exchange, "Authorization cookie is missing in request", HttpStatus.UNAUTHORIZED);
            }

            final String token = authCookie.getValue();

            try {
                jwtUtil.validateToken(token);
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                log.error("Token has expired for path: {}", request.getURI().getPath());
                return this.onError(exchange, "Authorization token has expired", HttpStatus.UNAUTHORIZED);
            } catch (io.jsonwebtoken.security.SignatureException e) {
                log.error("Invalid token signature for path: {}", request.getURI().getPath());
                return this.onError(exchange, "Invalid token signature", HttpStatus.UNAUTHORIZED);
            } catch (io.jsonwebtoken.MalformedJwtException e) {
                log.error("Malformed token for path: {}", request.getURI().getPath());
                return this.onError(exchange, "Malformed authorization token", HttpStatus.UNAUTHORIZED);
            } catch (IllegalArgumentException e) {
                log.error("Token validation error: {}", e.getMessage());
                return this.onError(exchange, e.getMessage(), HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                log.error("Error validating token: {}", e.getMessage());
                return this.onError(exchange, "Authorization token is invalid", HttpStatus.UNAUTHORIZED);
            }

            try {
                Claims claims = jwtUtil.extractAllClaims(token);
                String userId = claims.getSubject();
                String userRole = claims.get("role") != null ? String.valueOf(claims.get("role")) : "USER";
                String userEmail = claims.get("email") != null ? String.valueOf(claims.get("email")) : "unknown";

                log.info("Token validated for user: {} with role: {}", userId, userRole);

                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-ID", userId)
                        .header("X-User-Role", userRole)
                        .header("X-User-Email", userEmail)
                        .build();

                ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                return chain.filter(mutatedExchange);
            } catch (Exception e) {
                log.error("Error extracting claims from token: {}", e.getMessage());
                return this.onError(exchange, "Failed to process token", HttpStatus.UNAUTHORIZED);
            }
        }

        return chain.filter(exchange);
    }

    /**
     * Constructs an error response for authentication failures.
     * 
     * <p>
     * Creates a JSON-formatted error response with the specified message and HTTP
     * status.
     * This method is called when authentication fails due to missing, invalid, or
     * expired tokens.
     * 
     * @param exchange     the current server exchange
     * @param errorMessage the error message to include in the response
     * @param httpStatus   the HTTP status code to return
     * @return a Mono that writes the error response to the client
     */
    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonResponse = String.format("{\"error\":\"%s\",\"status\":%d}", errorMessage, httpStatus.value());
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}