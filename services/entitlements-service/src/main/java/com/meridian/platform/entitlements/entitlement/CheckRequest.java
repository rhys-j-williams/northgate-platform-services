package com.meridian.platform.entitlements.entitlement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record CheckRequest(
    @NotBlank String userHandle,
    @NotBlank @Pattern(regexp = "[a-z]+:[a-z]+", message = "permission is area:verb") String permission,
    String organisationId,
    @PositiveOrZero Long amountMinor) {
}
