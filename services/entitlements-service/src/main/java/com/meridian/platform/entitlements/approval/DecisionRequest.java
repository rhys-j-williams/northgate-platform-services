package com.meridian.platform.entitlements.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DecisionRequest(@NotBlank String decidedBy, @Size(max = 400) String reason) {
}
