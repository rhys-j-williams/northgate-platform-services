package com.meridian.platform.entitlements.entitlement;

import com.meridian.platform.entitlements.infra.ApiException;
import com.meridian.platform.entitlements.role.RoleCatalogue;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntitlementService {

    private final EntitlementRepository repository;
    private final RoleCatalogue roles;

    public EntitlementService(EntitlementRepository repository, RoleCatalogue roles) {
        this.repository = repository;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public List<Entitlement> forOrganisation(String organisationId) {
        return repository.findByOrganisationIdOrderByUserHandle(organisationId);
    }

    @Transactional(readOnly = true)
    public List<Entitlement> activeForUser(String userHandle) {
        return repository.findByUserHandleAndStatus(userHandle, "ACTIVE");
    }

    @Transactional(readOnly = true)
    public Entitlement require(String entitlementId) {
        return repository.findById(entitlementId)
            .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "no entitlement " + entitlementId));
    }

    /**
     * The check. Walk the user's active entitlements (optionally narrowed to an organisation),
     * pick the first role that carries the permission. Payment initiation above the per
     * transaction limit is a deny; initiation by a role flagged dual approval is an allow that
     * must go through the approvals queue. Approvers never need dual approval for approving,
     * that is what they are for, but they cannot approve their own initiation (checked in
     * bff-business, PLAT-1174, not here. TODO(PLAT-1660) move it here).
     */
    @Transactional(readOnly = true)
    public Decision check(CheckRequest req) {
        List<Entitlement> active = activeForUser(req.userHandle());
        if (req.organisationId() != null) {
            active = active.stream().filter(e -> req.organisationId().equals(e.getOrganisationId())).toList();
        }
        if (active.isEmpty()) {
            return Decision.deny("NO_ACTIVE_ENTITLEMENT");
        }
        Optional<Entitlement> match = active.stream()
            .filter(e -> roles.permissionsOf(e.getRoleCode()).contains(req.permission()))
            .findFirst();
        if (match.isEmpty()) {
            return Decision.deny("PERMISSION_NOT_HELD");
        }
        Entitlement e = match.get();
        if (RoleCatalogue.INITIATE_PAYMENTS.equals(req.permission()) && req.amountMinor() != null) {
            if (e.getLimitPerTxnMinor() != null && req.amountMinor() > e.getLimitPerTxnMinor()) {
                return Decision.deny("OVER_TXN_LIMIT");
            }
            if (e.isDualApprovalRequired()) {
                return Decision.dual(e, "ROLE_DUAL_APPROVAL");
            }
        }
        return Decision.allow(e);
    }

    @Transactional
    public Entitlement activate(String organisationId, String customerId, String userHandle, String roleCode,
                                boolean dualApproval, Long limitTxn, Long limitDay) {
        roles.require(roleCode);
        Optional<Entitlement> existing = repository
            .findByOrganisationIdAndUserHandleAndRoleCodeAndStatus(organisationId, userHandle, roleCode, "ACTIVE");
        if (existing.isPresent()) {
            throw ApiException.conflict("ALREADY_GRANTED", userHandle + " already holds " + roleCode);
        }
        Instant now = Instant.now();
        Entitlement e = new Entitlement();
        e.setEntitlementId(com.meridian.platform.entitlements.infra.Ids.next("ENT"));
        e.setOrganisationId(organisationId);
        e.setCustomerId(customerId);
        e.setUserHandle(userHandle);
        e.setRoleCode(roleCode);
        e.setDualApprovalRequired(dualApproval);
        e.setLimitPerTxnMinor(limitTxn);
        e.setLimitPerDayMinor(limitDay);
        e.setStatus("ACTIVE");
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return repository.save(e);
    }

    @Transactional
    public Entitlement revoke(String entitlementId) {
        Entitlement e = require(entitlementId);
        if (!"ACTIVE".equals(e.getStatus())) {
            throw ApiException.conflict("NOT_ACTIVE", entitlementId + " is " + e.getStatus());
        }
        e.setStatus("REVOKED");
        e.setUpdatedAt(Instant.now());
        return repository.save(e);
    }

    @Transactional(readOnly = true)
    public boolean holds(String userHandle, String organisationId, String permission) {
        return activeForUser(userHandle).stream()
            .filter(e -> organisationId.equals(e.getOrganisationId()))
            .anyMatch(e -> roles.permissionsOf(e.getRoleCode()).contains(permission));
    }
}
