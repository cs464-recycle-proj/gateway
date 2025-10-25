package com.greenloop.gateway.config;

import com.greenloop.gateway.filter.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class GatewayConfig {

        @Autowired
        private AuthenticationFilter filter;

        @Value("${auth.service.url}")
        private String authServiceUrl;

        @Value("${user.service.url}")
        private String userServiceUrl;

        @Value("${user.service.url}")
        private String eventServiceUrl;

        @Bean
        public RouteLocator routes(RouteLocatorBuilder builder) {
                log.info("Configuring routes with auth service URL: {}", authServiceUrl);

                return builder.routes()
                                .route("auth-service", r -> r.path("/api/auth/**")
                                                .filters(f -> f.filter(filter))
                                                .uri(authServiceUrl))
                                .route("verify-service", r -> r.path("/api/verify/**")
                                                .filters(f -> f.filter(filter))
                                                .uri(authServiceUrl))
                                .route("user-service", r -> r.path("/api/users/**")
                                                .filters(f -> f.filter(filter))
                                                .uri(userServiceUrl))
                                .route("event-service", r -> r.path("/api/events/**")
                                                .filters(f -> f.filter(filter))
                                                .uri(eventServiceUrl))
                                .build();
        }
}