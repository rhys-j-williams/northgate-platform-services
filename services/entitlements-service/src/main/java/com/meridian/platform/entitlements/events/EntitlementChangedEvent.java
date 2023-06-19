package com.meridian.platform.entitlements.events;

import java.time.Instant;

public record EntitlementChangedEvent(
    String eventType,
    String entitlementId,
    String organisationId,
    String userHandle,
    String roleCode,
    String requestedBy,
    String approvedBy,
    Instant occurredAt) {
}
