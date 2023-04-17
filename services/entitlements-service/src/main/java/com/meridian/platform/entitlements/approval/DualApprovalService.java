package com.meridian.platform.entitlements.approval;

import com.meridian.platform.entitlements.entitlement.Entitlement;
import com.meridian.platform.entitlements.entitlement.EntitlementService;
import com.meridian.platform.entitlements.events.EntitlementEventPublisher;
import com.meridian.platform.entitlements.infra.ApiException;
import com.meridian.platform.entitlements.infra.Ids;
import com.meridian.platform.entitlements.role.RoleCatalogue;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Four eyes on entitlement changes. Rules, in order (PLAT-1174, tightened after INC-2022-0871):
 *
 *  - requester must hold entitlements:manage in the organisation, else 403
 *  - changes to a non sensitive role (viewer, initiator, auditor) apply immediately
 *  - changes to a sensitive role (administrator, approver) create an APPROVAL_REQUEST
 *  - the approver must hold entitlements:manage in the organisation
 *  - the approver must not be the requester
 *  - the approver must not be the target user (you cannot approve your own promotion)
 *  - pending requests expire after meridian.approvals.ttl-hours
 *
 * There is no notion of "second approver" for revokes; a revoke of a sensitive role still needs
 * two people because a hostile admin revoking the only other approver is the same problem in
 * reverse.
 */
@Service
public class DualApprovalService {

    private static final Logger LOG = LoggerFactory.getLogger(DualApprovalService.class);

    private final ApprovalRequestRepository requests;
    private final EntitlementService entitlements;
    private final RoleCatalogue roles;
    private final EntitlementEventPublisher publisher;
    private final ApprovalProperties props;

    public DualApprovalService(ApprovalRequestRepository requests, EntitlementService entitlements,
                               RoleCatalogue roles, EntitlementEventPublisher publisher, ApprovalProperties props) {
        this.requests = requests;
        this.entitlements = entitlements;
        this.roles = roles;
        this.publisher = publisher;
        this.props = props;
    }

    @Transactional
    public ChangeOutcome grant(String organisationId, ChangeRequest req) {
        requireManager(req.requestedBy(), organisationId);
        roles.require(req.roleCode());
        if (!roles.isSensitive(req.roleCode())) {
            Entitlement e = entitlements.activate(organisationId, req.customerId(), req.userHandle(), req.roleCode(),
                req.dualApprovalRequired(), req.limitPerTxnMinor(), req.limitPerDayMinor());
            publisher.granted(e, req.requestedBy(), null);
            return new ChangeOutcome(e, null);
        }
        ApprovalRequest ar = newRequest(organisationId, "GRANT", req.userHandle(), req.roleCode(), req.requestedBy());
        ar.setReason(packGrant(req));
        return new ChangeOutcome(null, requests.save(ar));
    }

    @Transactional
    public ChangeOutcome revoke(String organisationId, String entitlementId, String requestedBy) {
        requireManager(requestedBy, organisationId);
        Entitlement target = entitlements.require(entitlementId);
        if (!organisationId.equals(target.getOrganisationId())) {
            throw ApiException.notFound("ENTITLEMENT_NOT_FOUND", "not in organisation " + organisationId);
        }
        if (!roles.isSensitive(target.getRoleCode())) {
            Entitlement e = entitlements.revoke(entitlementId);
            publisher.revoked(e, requestedBy, null);
            return new ChangeOutcome(e, null);
        }
        ApprovalRequest ar = newRequest(organisationId, "REVOKE", target.getUserHandle(), target.getRoleCode(), requestedBy);
        ar.setTargetEntitlementId(entitlementId);
        return new ChangeOutcome(null, requests.save(ar));
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> pending(String organisationId) {
        return requests.findByOrganisationIdAndStatusOrderByRequestedAt(organisationId, "PENDING");
    }

    @Transactional
    public ApprovalRequest approve(String requestId, DecisionRequest decision) {
        ApprovalRequest ar = requirePending(requestId);
        requireManager(decision.decidedBy(), ar.getOrganisationId());
        if (decision.decidedBy().equals(ar.getRequestedBy())) {
            throw ApiException.forbidden("SELF_APPROVAL", "requester cannot approve their own request");
        }
        if (decision.decidedBy().equals(ar.getTargetUser())) {
            throw ApiException.forbidden("SELF_APPROVAL", "target user cannot approve a change to themselves");
        }
        Entitlement e;
        if ("GRANT".equals(ar.getAction())) {
            GrantDetails d = unpackGrant(ar.getReason());
            e = entitlements.activate(ar.getOrganisationId(), d.customerId(), ar.getTargetUser(), ar.getRoleCode(),
                d.dual(), d.limitTxn(), d.limitDay());
            publisher.granted(e, ar.getRequestedBy(), decision.decidedBy());
        } else {
            e = entitlements.revoke(ar.getTargetEntitlementId());
            publisher.revoked(e, ar.getRequestedBy(), decision.decidedBy());
        }
        ar.setTargetEntitlementId(e.getEntitlementId());
        ar.setStatus("APPROVED");
        ar.setDecidedBy(decision.decidedBy());
        ar.setDecidedAt(Instant.now());
        ar.setReason(decision.reason());
        return requests.save(ar);
    }

    @Transactional
    public ApprovalRequest reject(String requestId, DecisionRequest decision) {
        ApprovalRequest ar = requirePending(requestId);
        requireManager(decision.decidedBy(), ar.getOrganisationId());
        ar.setStatus("REJECTED");
        ar.setDecidedBy(decision.decidedBy());
        ar.setDecidedAt(Instant.now());
        ar.setReason(decision.reason());
        return requests.save(ar);
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    @Transactional
    public int expireStale() {
        List<ApprovalRequest> stale = requests.findByStatusAndExpiresAtBefore("PENDING", Instant.now());
        for (ApprovalRequest ar : stale) {
            ar.setStatus("EXPIRED");
            ar.setDecidedAt(Instant.now());
            ar.setReason("expired after " + props.ttlHours() + "h");
        }
        requests.saveAll(stale);
        if (!stale.isEmpty()) {
            LOG.info("expired {} approval requests", stale.size());
        }
        return stale.size();
    }

    private ApprovalRequest requirePending(String requestId) {
        ApprovalRequest ar = requests.findById(requestId)
            .orElseThrow(() -> ApiException.notFound("APPROVAL_NOT_FOUND", "no request " + requestId));
        if (!"PENDING".equals(ar.getStatus())) {
            throw ApiException.conflict("APPROVAL_NOT_PENDING", requestId + " is " + ar.getStatus());
        }
        if (ar.getExpiresAt().isBefore(Instant.now())) {
            ar.setStatus("EXPIRED");
            requests.save(ar);
            throw ApiException.conflict("APPROVAL_EXPIRED", requestId + " expired at " + ar.getExpiresAt());
        }
        return ar;
    }

    private void requireManager(String userHandle, String organisationId) {
        if (!entitlements.holds(userHandle, organisationId, RoleCatalogue.MANAGE_ENTITLEMENTS)) {
            throw ApiException.forbidden("NOT_ENTITLEMENT_MANAGER",
                userHandle + " does not hold " + RoleCatalogue.MANAGE_ENTITLEMENTS + " in " + organisationId);
        }
    }

    private ApprovalRequest newRequest(String organisationId, String action, String target, String role, String by) {
        ApprovalRequest ar = new ApprovalRequest();
        ar.setRequestId(Ids.next("APR"));
        ar.setOrganisationId(organisationId);
        ar.setAction(action);
        ar.setTargetUser(target);
        ar.setRoleCode(role);
        ar.setRequestedBy(by);
        ar.setRequestedAt(Instant.now());
        ar.setExpiresAt(Instant.now().plus(Duration.ofHours(props.ttlHours())));
        ar.setStatus("PENDING");
        return ar;
    }

    // The grant parameters ride in REASON until approval, pipe separated. Ugly; there was no
    // appetite for another table for four fields (PLAT-1174 review thread). Cleared on decision.
    static String packGrant(ChangeRequest req) {
        return String.join("|", req.customerId(), String.valueOf(req.dualApprovalRequired()),
            req.limitPerTxnMinor() == null ? "" : req.limitPerTxnMinor().toString(),
            req.limitPerDayMinor() == null ? "" : req.limitPerDayMinor().toString());
    }

    static GrantDetails unpackGrant(String packed) {
        String[] p = (packed == null ? "" : packed).split("\\|", -1);
        if (p.length < 4) {
            return new GrantDetails("UNKNOWN", false, null, null);
        }
        return new GrantDetails(p[0], Boolean.parseBoolean(p[1]),
            p[2].isEmpty() ? null : Long.valueOf(p[2]), p[3].isEmpty() ? null : Long.valueOf(p[3]));
    }

    record GrantDetails(String customerId, boolean dual, Long limitTxn, Long limitDay) {
    }
}
