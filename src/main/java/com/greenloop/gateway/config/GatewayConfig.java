package com.greenloop.gateway.config;

import com.greenloop.gateway.filter.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * Gateway configuration that defines routing rules for microservices.
 * 
 * <p>
 * This configuration class sets up Spring Cloud Gateway routes to forward
 * incoming requests to appropriate downstream microservices. Each route:
 * <ul>
 * <li>Matches specific URL path patterns</li>
 * <li>Applies the authentication filter for JWT validation</li>
 * <li>Forwards requests to the corresponding service URL</li>
 * </ul>
 * 
 * <p>
 * <b>Configured Routes:</b>
 * <ul>
 * <li><b>auth-service</b>: /api/auth/** → Authentication and user
 * management</li>
 * <li><b>verify-service</b>: /api/verify/** → OTP verification</li>
 * <li><b>user-service</b>: /api/users/** → User profile operations</li>
 * <li><b>event-service</b>: /api/events/** → Event management</li>
 * </ul>
 * 
 * <p>
 * Service URLs are injected from application properties and can be
 * configured for different environments (local, staging, production).
 * 
 * @author GreenLoop Team
 * @version 1.0
 * @see AuthenticationFilter
 */
@Configuration
@Slf4j
public class GatewayConfig {

        @Autowired
        private AuthenticationFilter filter;

        @Value("${auth.service.url}")
        private String authServiceUrl;

        @Value("${user.service.url}")
        private String userServiceUrl;

        @Value("${event.service.url}")
        private String eventServiceUrl;

        @Value("${gamification.service.url}")
        private String gamificationServiceUrl;

        /**
         * Configures the route locator with mappings for all microservices.
         * 
         * <p>
         * This bean defines how incoming requests are routed based on URL patterns.
         * All routes include the {@link AuthenticationFilter} which handles JWT
         * validation
         * and user context enrichment before forwarding to downstream services.
         * 
         * @param builder the RouteLocatorBuilder for defining routes
         * @return configured RouteLocator with all service routes
         */
        @Bean
        public RouteLocator routes(RouteLocatorBuilder builder) {
                log.info("🚀 Configuring Gateway Routes:");
                log.info("  - Auth Service URL: {}", authServiceUrl);
                log.info("  - User Service URL: {}", userServiceUrl);
                log.info("  - Event Service URL: {}", eventServiceUrl);
                log.info("  - Gamification Service URL: {}", gamificationServiceUrl);

                return builder.routes()
                                .route("auth-service", r -> r.path("/api/auth/**", "/api/verify/**")
                                                .filters(f -> f.filter(filter))
                                                .uri(authServiceUrl))
                                .route("user-service", r -> r.path("/api/users/**")
                                                .filters(f -> f.filter(filter))
                                                .uri(userServiceUrl))
                                .route("event-service", r -> r.path("/api/events/**")
                                                .filters(f -> f.filter(filter))
                                                .uri(eventServiceUrl))
                                .route("gamification-service", r -> r.path("/gamification/**", "/api/v1/gamification/**")
                                                .filters(f -> f.filter(filter)
                                                                .filter((exchange, chain) -> {
                                                                        log.info("🎮 Gamification Route - Original URI: {}",
                                                                                        exchange.getRequest().getURI());
                                                                        log.info("🎮 Gamification Route - Path: {}",
                                                                                        exchange.getRequest().getPath()
                                                                                                        .value());
                                                                        log.info("🎮 Gamification Route - Method: {}",
                                                                                        exchange.getRequest().getMethod());
                                                                        log.info("🎮 Gamification Route - Headers: {}",
                                                                                        exchange.getRequest().getHeaders());
                                                                        log.info("🎮 Gamification Route - Target Base URL: {}",
                                                                                        gamificationServiceUrl);
                                                                        log.info("🎮 Gamification Route - Full Target: {}{}",
                                                                                        gamificationServiceUrl,
                                                                                        exchange.getRequest().getPath()
                                                                                                        .value());
                                                                        
                                                                        // Log the actual route attributes
                                                                        exchange.getAttributes().forEach((key, value) -> {
                                                                                if (key.toString().contains("gateway") || key.toString().contains("uri")) {
                                                                                        log.info("🎮 Route Attribute: {} = {}", key, value);
                                                                                }
                                                                        });
                                                                        
                                                                        return chain.filter(exchange).doOnSuccess(v -> {
                                                                                log.info("🎮 Gamification Response - Status: {}",
                                                                                                exchange.getResponse()
                                                                                                                .getStatusCode());
                                                                                log.info("🎮 Gamification Response - Headers: {}",
                                                                                                exchange.getResponse().getHeaders());
                                                                        });
                                                                }))
                                                .uri(gamificationServiceUrl))
                                .build();
        }
}