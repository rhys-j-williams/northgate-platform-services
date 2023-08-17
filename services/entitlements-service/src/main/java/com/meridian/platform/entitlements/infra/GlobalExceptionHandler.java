package com.meridian.platform.entitlements.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> api(ApiException e) {
        return ResponseEntity.status(e.getStatus())
            .body(ApiError.of(e.getCode(), e.getMessage(), e.getStatus().value(), MDC.get(CorrelationIdFilter.MDC_KEY)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .sorted()
            .findFirst()
            .orElse("validation failed");
        return ResponseEntity.badRequest()
            .body(ApiError.of("VALIDATION", msg, 400, MDC.get(CorrelationIdFilter.MDC_KEY)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unhandled(Exception e) {
        LOG.error("unhandled_exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.of("INTERNAL", "internal error", 500, MDC.get(CorrelationIdFilter.MDC_KEY)));
    }
}
