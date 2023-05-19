package com.meridian.platform.entitlements.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.meridian.platform.entitlements.entitlement.EntitlementService;
import com.meridian.platform.entitlements.infra.ApiException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Was a Testcontainers Oracle XE test until PLAT-1601; the XE image took four minutes to start on
 * the build agents and the DBAs would not licence it anyway. H2 Oracle mode it is.
 */
@SpringBootTest
@Transactional
class DualApprovalServiceTest {

    static final String ORG = "ORG-900000001";

    @Autowired
    DualApprovalService approvals;

    @Autowired
    EntitlementService entitlements;

    @Autowired
    ApprovalRequestRepository requests;

    private ChangeRequest grant(String user, String role, String by) {
        return new ChangeRequest(user, "CUS-100000", role, by, false, null, null);
    }

    @Test
    void nonSensitiveGrantAppliesImmediately() {
        ChangeOutcome out = approvals.grant(ORG, grant("fresh.viewer", "viewer", "test.admin"));
        assertThat(out.isPending()).isFalse();
        assertThat(out.applied().getRoleCode()).isEqualTo("viewer");
    }

    @Test
    void sensitiveGrantQueuesForApproval() {
        ChangeOutcome out = approvals.grant(ORG, grant("fresh.approver", "approver", "test.admin"));
        assertThat(out.isPending()).isTrue();
        assertThat(out.pending().getStatus()).isEqualTo("PENDING");
        assertThat(approvals.pending(ORG)).extracting(ApprovalRequest::getTargetUser).contains("fresh.approver");
        assertThat(entitlements.activeForUser("fresh.approver")).isEmpty();
    }

    @Test
    void requesterWithoutManagePermissionIsRefused() {
        assertThatThrownBy(() -> approvals.grant(ORG, grant("someone", "viewer", "test.viewer")))
            .isInstanceOf(ApiException.class)
            .extracting("code").isEqualTo("NOT_ENTITLEMENT_MANAGER");
    }








    @Test
    void packUnpackRoundTrip() {
        ChangeRequest req = new ChangeRequest("u.v", "CUS-1", "approver", "a.b", true, null, 5L);
        DualApprovalService.GrantDetails d = DualApprovalService.unpackGrant(DualApprovalService.packGrant(req));
        assertThat(d.customerId()).isEqualTo("CUS-1");
        assertThat(d.dual()).isTrue();
        assertThat(d.limitTxn()).isNull();
        assertThat(d.limitDay()).isEqualTo(5L);
    }
}
