package com.meridian.platform.entitlements.entitlement;

/**
 * Outcome of an entitlement check. REQUIRES_DUAL_APPROVAL is an allow with strings attached: the
 * caller (bff-business) must route the action through the approvals queue.
 */
public record Decision(Outcome outcome, String reason, String entitlementId, String roleCode) {

    public enum Outcome { ALLOW, DENY, REQUIRES_DUAL_APPROVAL }

    public static Decision allow(Entitlement e) {
        return new Decision(Outcome.ALLOW, null, e.getEntitlementId(), e.getRoleCode());
    }

    public static Decision dual(Entitlement e, String reason) {
        return new Decision(Outcome.REQUIRES_DUAL_APPROVAL, reason, e.getEntitlementId(), e.getRoleCode());
    }

    public static Decision deny(String reason) {
        return new Decision(Outcome.DENY, reason, null, null);
    }

    public boolean permitted() {
        return outcome != Outcome.DENY;
    }
}
