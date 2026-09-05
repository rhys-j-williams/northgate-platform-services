package com.meridian.platform.entitlements.entitlement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ENTITLEMENT")
public class Entitlement {

    @Id
    @Column(name = "ENTITLEMENT_ID", length = 20)
    private String entitlementId;

    @Column(name = "ORGANISATION_ID", nullable = false, length = 20)
    private String organisationId;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 20)
    private String customerId;

    @Column(name = "USER_HANDLE", nullable = false, length = 64)
    private String userHandle;

    @Column(name = "ROLE_CODE", nullable = false, length = 32)
    private String roleCode;

    @Column(name = "DUAL_APPROVAL_REQUIRED", nullable = false)
    private boolean dualApprovalRequired;

    @Column(name = "LIMIT_PER_TXN_MINOR")
    private Long limitPerTxnMinor;

    @Column(name = "LIMIT_PER_DAY_MINOR")
    private Long limitPerDayMinor;

    @Column(name = "STATUS", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "CREATED_AT", nullable = false)
    private Instant createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private Instant updatedAt;

    public String getEntitlementId() {
        return entitlementId;
    }

    public void setEntitlementId(String entitlementId) {
        this.entitlementId = entitlementId;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public boolean isDualApprovalRequired() {
        return dualApprovalRequired;
    }

    public void setDualApprovalRequired(boolean dualApprovalRequired) {
        this.dualApprovalRequired = dualApprovalRequired;
    }

    public Long getLimitPerTxnMinor() {
        return limitPerTxnMinor;
    }

    public void setLimitPerTxnMinor(Long limitPerTxnMinor) {
        this.limitPerTxnMinor = limitPerTxnMinor;
    }

    public Long getLimitPerDayMinor() {
        return limitPerDayMinor;
    }

    public void setLimitPerDayMinor(Long limitPerDayMinor) {
        this.limitPerDayMinor = limitPerDayMinor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
