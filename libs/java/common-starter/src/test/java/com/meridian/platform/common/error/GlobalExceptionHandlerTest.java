package com.meridian.platform.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsApiExceptionToStandardBody() {
        ResponseEntity<ApiError> response = handler.handleApi(
            ApiException.conflict("DUPLICATE_IDEMPOTENCY_KEY", "already posted"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_IDEMPOTENCY_KEY");
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getCorrelationId()).isNotBlank();
    }

    @Test
    void neverLeaksUnexpectedExceptionMessages() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
            new IllegalStateException("ORA-00001: unique constraint violated on CUST_SSN"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).doesNotContain("ORA-");
    }
}
