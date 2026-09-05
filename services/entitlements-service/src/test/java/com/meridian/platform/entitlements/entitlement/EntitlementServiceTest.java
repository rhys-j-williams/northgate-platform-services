package com.meridian.platform.entitlements.entitlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.meridian.platform.entitlements.infra.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EntitlementServiceTest {

    @Autowired
    EntitlementService service;

    @Test
    void administratorHoldsEntitlementsManage() {
        Decision d = service.check(new CheckRequest("test.admin", "entitlements:manage", "ORG-900000001", null));
        assertThat(d.outcome()).isEqualTo(Decision.Outcome.ALLOW);
        assertThat(d.roleCode()).isEqualTo("administrator");
    }

    @Test
    void viewerCannotInitiatePayments() {
        Decision d = service.check(new CheckRequest("test.viewer", "payments:initiate", null, 100L));
        assertThat(d.outcome()).isEqualTo(Decision.Outcome.DENY);
        assertThat(d.reason()).isEqualTo("PERMISSION_NOT_HELD");
    }

    @Test
    void unknownUserIsDenied() {
        Decision d = service.check(new CheckRequest("nobody.here", "accounts:view", null, null));
        assertThat(d.permitted()).isFalse();
        assertThat(d.reason()).isEqualTo("NO_ACTIVE_ENTITLEMENT");
    }

    @Test
    void initiatorOverLimitIsDenied() {
        Decision d = service.check(new CheckRequest("test.initiator", "payments:initiate", null, 5_000_001L));
        assertThat(d.reason()).isEqualTo("OVER_TXN_LIMIT");
    }

    @Test
    void initiatorUnderLimitWithDualApprovalFlagRoutesToQueue() {
        Decision d = service.check(new CheckRequest("test.initiator", "payments:initiate", null, 4_999_999L));
        assertThat(d.outcome()).isEqualTo(Decision.Outcome.REQUIRES_DUAL_APPROVAL);
        assertThat(d.permitted()).isTrue();
    }


    @Test
    void activateRejectsUnknownRole() {
        assertThatThrownBy(() -> service.activate("ORG-900000001", "CUS-1", "x.y", "superuser", false, null, null))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("superuser");
    }


}
