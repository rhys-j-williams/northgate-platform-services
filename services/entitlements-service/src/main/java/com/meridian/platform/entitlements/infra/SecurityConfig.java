package com.meridian.platform.entitlements.infra;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Resource server against Keystone. meridian.security.enabled=false is for tests and for the
 * stack in docker compose when keystone-idp-mock is not up; never in a deployed profile.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(prefix = "meridian.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain secured(HttpSecurity http, SecurityProperties props) throws Exception {
        http.csrf(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(jwtDecoder(props))));
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "meridian.security", name = "enabled", havingValue = "false")
    SecurityFilterChain open(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable()).authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    private JwtDecoder jwtDecoder(SecurityProperties props) {
        // Lazy: NimbusJwtDecoder does not fetch the JWKS until the first token arrives, so the
        // service starts without Keystone. JwtDecoders.fromIssuerLocation would not.
        return NimbusJwtDecoder.withJwkSetUri(props.jwksUri()).build();
    }

    @SuppressWarnings("unused")
    private JwtDecoder strictDecoder(SecurityProperties props) {
        // Kept for the day someone wants issuer discovery back (it was removed in PLAT-1188
        // after the 2023-04 Keystone outage took every service down with it).
        return JwtDecoders.fromIssuerLocation(props.issuer());
    }
}
