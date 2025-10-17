package com.greenloop.gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouterValidator {

    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/health",
            "/api/verify/check-otp",
            "/api/verify/send-otp");

    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();
        return openApiEndpoints.stream().noneMatch(path::equals);
    };
}
