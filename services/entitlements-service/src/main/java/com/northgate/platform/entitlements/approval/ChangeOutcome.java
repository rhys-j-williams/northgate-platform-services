package com.northgate.platform.entitlements.approval;

import com.northgate.platform.entitlements.entitlement.Entitlement;

/** Either it was applied straight away, or it is sitting in the queue. Exactly one is non null. */
public record ChangeOutcome(Entitlement applied, ApprovalRequest pending) {

    public boolean isPending() {
        return pending != null;
    }
}
