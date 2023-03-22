package com.meridian.platform.entitlements.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "APPROVAL_REQUEST")
public class ApprovalRequest {

    @Id
    @Column(name = "REQUEST_ID", length = 20)
    private String requestId;

    @Column(name = "ORGANISATION_ID", nullable = false, length = 20)
    private String organisationId;

    @Column(name = "ACTION", nullable = false, length = 16)
    private String action;

    @Column(name = "TARGET_USER", nullable = false, length = 64)
    private String targetUser;

    @Column(name = "ROLE_CODE", nullable = false, length = 32)
    private String roleCode;

    @Column(name = "TARGET_ENTITLEMENT_ID", length = 20)
    private String targetEntitlementId;

    @Column(name = "REQUESTED_BY", nullable = false, length = 64)
    private String requestedBy;

    @Column(name = "REQUESTED_AT", nullable = false)
    private Instant requestedAt;

    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;

    @Column(name = "STATUS", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "DECIDED_BY", length = 64)
    private String decidedBy;

    @Column(name = "DECIDED_AT")
    private Instant decidedAt;

    @Column(name = "REASON", length = 400)
    private String reason;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getTargetEntitlementId() {
        return targetEntitlementId;
    }

    public void setTargetEntitlementId(String targetEntitlementId) {
        this.targetEntitlementId = targetEntitlementId;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
