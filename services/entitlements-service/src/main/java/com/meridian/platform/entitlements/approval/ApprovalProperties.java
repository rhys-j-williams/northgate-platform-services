package com.meridian.platform.entitlements.approval;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meridian.approvals")
public record ApprovalProperties(int ttlHours) {
}
