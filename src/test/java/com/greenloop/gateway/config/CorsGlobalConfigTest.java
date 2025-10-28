package com.greenloop.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CORS configuration.
 * Verifies that CORS filter can be properly configured.
 */
class CorsGlobalConfigTest {

    @Test
    void testCorsWebFilterCreation() {
        // Arrange
        CorsGlobalConfig config = new CorsGlobalConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://localhost:3000");

        // Act
        CorsWebFilter filter = config.corsWebFilter();

        // Assert
        assertNotNull(filter, "CorsWebFilter should be created successfully");
    }

    @Test
    void testCorsGlobalConfigBeanCreation() {
        // Arrange & Act
        CorsGlobalConfig config = new CorsGlobalConfig();

        // Assert
        assertNotNull(config, "CorsGlobalConfig should be instantiable");
    }
}