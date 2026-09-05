package com.meridian.platform.common.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meridian.security")
public class MeridianSecurityProperties {

    /** Set to false in unit tests and in the local profile of services that have no user facing surface. */
    private boolean enabled = true;
    /** Keystone JWKS. keystone-idp-mock serves it on 4400 locally. */
    private String jwksUri = "http://localhost:4400/.well-known/jwks.json";
    private String issuer = "http://localhost:4400";
    private List<String> permitAll = new ArrayList<>(List.of("/actuator/**", "/internal/**"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public List<String> getPermitAll() {
        return permitAll;
    }

    public void setPermitAll(List<String> permitAll) {
        this.permitAll = permitAll;
    }
}
