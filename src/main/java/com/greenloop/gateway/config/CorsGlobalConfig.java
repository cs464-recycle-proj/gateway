package com.greenloop.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Global CORS (Cross-Origin Resource Sharing) configuration for the gateway.
 * 
 * <p>
 * This configuration enables the API gateway to accept requests from
 * specified frontend origins, allowing cross-origin requests with credentials.
 * 
 * <p>
 * <b>CORS Settings:</b>
 * <ul>
 * <li><b>Allowed Origins</b>: Configured via application properties (default:
 * http://localhost:3000)</li>
 * <li><b>Allowed Headers</b>: All headers (*)</li>
 * <li><b>Allowed Methods</b>: All HTTP methods (GET, POST, PUT, DELETE,
 * etc.)</li>
 * <li><b>Allow Credentials</b>: Enabled (cookies and authorization
 * headers)</li>
 * </ul>
 * 
 * <p>
 * <b>Security Note:</b> In production, the allowed origins should be restricted
 * to specific domains rather than using wildcards or localhost URLs.
 * 
 * @author GreenLoop Team
 * @version 1.0
 */
@Configuration
public class CorsGlobalConfig {

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    /**
     * Configures the CORS web filter for reactive Spring Cloud Gateway.
     * 
     * <p>
     * This filter is applied globally to all routes, enabling cross-origin
     * requests from the configured frontend application. It ensures that
     * preflight OPTIONS requests are handled correctly and that cookies
     * (including JWT tokens) are allowed in cross-origin requests.
     * 
     * @return configured CorsWebFilter for handling CORS requests
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin(allowedOrigins);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
