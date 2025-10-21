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
            } catch (Exception e) {
                log.error("Error validating token: {}", e.getMessage());
                return this.onError(exchange, "Authorization token is invalid", HttpStatus.UNAUTHORIZED);
            }

            try {
                Claims claims = jwtUtil.extractAllClaims(token);
                String userId = claims.getSubject();
                String userRole = claims.get("role") != null ? String.valueOf(claims.get("role")) : "USER";

                log.info("Token validated for user: {} with role: {}", userId, userRole);

                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-ID", userId)
                        .header("X-User-Role", userRole)
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

    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonResponse = String.format("{\"error\":\"%s\",\"status\":%d}", errorMessage, httpStatus.value());
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}