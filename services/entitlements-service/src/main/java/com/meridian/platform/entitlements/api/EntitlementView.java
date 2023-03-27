package com.meridian.platform.entitlements.api;

import java.util.Set;

public record EntitlementView(
    String entitlementId,
    String organisationId,
    String customerId,
    String userHandle,
    String role,
    Set<String> permissions,
    boolean dualApprovalRequired,
    Long limitPerTransactionMinor,
    Long limitPerDayMinor,
    String status) {
}
