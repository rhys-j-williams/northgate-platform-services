package com.northgate.platform.entitlements.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "northgate.security")
public record SecurityProperties(boolean enabled, String jwksUri, String issuer) {
}
