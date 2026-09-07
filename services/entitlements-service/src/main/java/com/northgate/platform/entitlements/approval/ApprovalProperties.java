package com.northgate.platform.entitlements.approval;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "northgate.approvals")
public record ApprovalProperties(int ttlHours) {
}
