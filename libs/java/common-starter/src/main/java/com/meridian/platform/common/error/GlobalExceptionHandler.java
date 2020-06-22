package com.meridian.platform.common.error;

import com.meridian.platform.common.correlation.CorrelationId;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        if (ex.getStatus().is5xxServerError()) {
            LOG.error("api_error code={} status={}", ex.getCode(), ex.getStatus().value(), ex);
        } else {
            LOG.warn("api_error code={} status={} message={}", ex.getCode(), ex.getStatus().value(),
                ex.getMessage());
        }
        return ResponseEntity.status(ex.getStatus())
            .body(new ApiError(ex.getCode(), ex.getMessage(), ex.getStatus().value(),
                CorrelationId.current(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ApiError.FieldViolation(fe.getField(), fe.getDefaultMessage()))
            .collect(Collectors.toList());
        return ResponseEntity.badRequest()
            .body(new ApiError("VALIDATION_FAILED", "Request failed validation", 400,
                CorrelationId.current(), violations));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        LOG.error("unhandled_exception", ex);
        // Deliberately no message from the exception: GIS-STD-014 section 9.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiError("INTERNAL_ERROR", "An unexpected error occurred", 500,
                CorrelationId.current(), null));
    }
}
