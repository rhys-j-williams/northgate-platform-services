package com.meridian.platform.entitlements.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meridian.security")
public record SecurityProperties(boolean enabled, String jwksUri, String issuer) {
}
