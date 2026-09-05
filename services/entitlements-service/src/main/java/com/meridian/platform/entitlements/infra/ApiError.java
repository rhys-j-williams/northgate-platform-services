package com.meridian.platform.entitlements.infra;

import java.time.Instant;

public record ApiError(String code, String message, int status, String correlationId, Instant timestamp) {

    public static ApiError of(String code, String message, int status, String correlationId) {
        return new ApiError(code, message, status, correlationId, Instant.now());
    }
}
