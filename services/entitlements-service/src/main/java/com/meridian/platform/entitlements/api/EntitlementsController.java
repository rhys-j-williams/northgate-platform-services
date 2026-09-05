package com.meridian.platform.entitlements.api;

import com.meridian.platform.entitlements.approval.ApprovalRequest;
import com.meridian.platform.entitlements.approval.ChangeOutcome;
import com.meridian.platform.entitlements.approval.ChangeRequest;
import com.meridian.platform.entitlements.approval.DecisionRequest;
import com.meridian.platform.entitlements.approval.DualApprovalService;
import com.meridian.platform.entitlements.entitlement.CheckRequest;
import com.meridian.platform.entitlements.entitlement.Decision;
import com.meridian.platform.entitlements.entitlement.Entitlement;
import com.meridian.platform.entitlements.entitlement.EntitlementService;
import com.meridian.platform.entitlements.role.RoleCatalogue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/entitlements/v1")
@Validated
public class EntitlementsController {

    private final EntitlementService entitlements;
    private final DualApprovalService approvals;
    private final RoleCatalogue roles;

    public EntitlementsController(EntitlementService entitlements, DualApprovalService approvals, RoleCatalogue roles) {
        this.entitlements = entitlements;
        this.approvals = approvals;
        this.roles = roles;
    }

    @GetMapping("/roles")
    public List<Map<String, Object>> roles() {
        return roles.all().stream()
            .map(r -> Map.<String, Object>of("roleCode", r.getRoleCode(), "description", r.getDescription(),
                "permissions", r.permissionSet(), "sensitive", r.isSensitive()))
            .toList();
    }

    @GetMapping("/organisations/{organisationId}")
    public List<EntitlementView> organisation(@PathVariable String organisationId) {
        return entitlements.forOrganisation(organisationId).stream().map(this::view).toList();
    }

    @GetMapping("/users/{userHandle}")
    public List<EntitlementView> user(@PathVariable String userHandle) {
        return entitlements.activeForUser(userHandle).stream().map(this::view).toList();
    }

    @PostMapping("/check")
    public Decision check(@Valid @RequestBody CheckRequest req) {
        return entitlements.check(req);
    }

    @PostMapping("/organisations/{organisationId}/grants")
    public ResponseEntity<?> grant(@PathVariable String organisationId, @Valid @RequestBody ChangeRequest req) {
        return outcome(approvals.grant(organisationId, req));
    }

    @PostMapping("/organisations/{organisationId}/entitlements/{entitlementId}/revoke")
    public ResponseEntity<?> revoke(@PathVariable String organisationId, @PathVariable String entitlementId,
                                    @RequestParam @NotBlank String requestedBy) {
        return outcome(approvals.revoke(organisationId, entitlementId, requestedBy));
    }

    @GetMapping("/approvals")
    public List<ApprovalRequest> pending(@RequestParam @NotBlank String organisationId) {
        return approvals.pending(organisationId);
    }

    @PostMapping("/approvals/{requestId}/approve")
    public ApprovalRequest approve(@PathVariable String requestId, @Valid @RequestBody DecisionRequest decision) {
        return approvals.approve(requestId, decision);
    }

    @PostMapping("/approvals/{requestId}/reject")
    public ApprovalRequest reject(@PathVariable String requestId, @Valid @RequestBody DecisionRequest decision) {
        return approvals.reject(requestId, decision);
    }

    private ResponseEntity<?> outcome(ChangeOutcome out) {
        if (out.isPending()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(out.pending());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(view(out.applied()));
    }

    private EntitlementView view(Entitlement e) {
        return new EntitlementView(e.getEntitlementId(), e.getOrganisationId(), e.getCustomerId(), e.getUserHandle(),
            e.getRoleCode(), roles.permissionsOf(e.getRoleCode()), e.isDualApprovalRequired(),
            e.getLimitPerTxnMinor(), e.getLimitPerDayMinor(), e.getStatus());
    }
}
