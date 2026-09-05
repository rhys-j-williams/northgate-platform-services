package com.meridian.platform.entitlements.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeRequest(
    @NotBlank @Pattern(regexp = "[a-z][a-z0-9._-]{2,63}") String userHandle,
    @NotBlank String customerId,
    @NotBlank @Pattern(regexp = "[a-z]{3,32}") String roleCode,
    @NotBlank String requestedBy,
    boolean dualApprovalRequired,
    @PositiveOrZero Long limitPerTxnMinor,
    @PositiveOrZero Long limitPerDayMinor) {
}
