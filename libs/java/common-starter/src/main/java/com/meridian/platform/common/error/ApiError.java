package com.meridian.platform.common.error;

import java.time.Instant;
import java.util.List;

/**
 * The standard error body. Agreed with the digital channels in 2020 (PLAT-812) and unchanged since,
 * because retail-web's error mapping interceptor switches on {@code code}. Add fields, never remove.
 */
public class ApiError {

    private final String code;
    private final String message;
    private final int status;
    private final String correlationId;
    private final Instant timestamp;
    private final List<FieldViolation> violations;

    public ApiError(String code, String message, int status, String correlationId,
                    List<FieldViolation> violations) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.correlationId = correlationId;
        this.timestamp = Instant.now();
        this.violations = violations == null ? List.of() : violations;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }

    public static class FieldViolation {
        private final String field;
        private final String reason;

        public FieldViolation(String field, String reason) {
            this.field = field;
            this.reason = reason;
        }

        public String getField() {
            return field;
        }

        public String getReason() {
            return reason;
        }
    }
}
