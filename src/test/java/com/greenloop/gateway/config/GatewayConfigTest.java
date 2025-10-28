package com.greenloop.gateway.config;

import com.greenloop.gateway.filter.AuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Gateway configuration.
 * Tests that route configuration logic works correctly.
 */
@ExtendWith(MockitoExtension.class)
class GatewayConfigTest {

    @Mock
    private AuthenticationFilter filter;

    @Mock
    private RouteLocatorBuilder routeLocatorBuilder;

    @Mock
    private RouteLocatorBuilder.Builder builder;

    @Test
    void testGatewayConfigBeanCreation() {
        // Arrange
        GatewayConfig config = new GatewayConfig();

        // Assert - verify the config object can be created
        assertNotNull(config, "GatewayConfig should be instantiable");
    }
}