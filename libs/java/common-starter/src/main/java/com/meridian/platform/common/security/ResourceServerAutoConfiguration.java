package com.meridian.platform.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Resource server wiring for the Keystone JWKS. Services are stateless bearer token consumers; the
 * step up (mfa_at) check is the caller's problem, not the starter's. The decoder fetches the JWKS
 * lazily on the first token so a service still starts when Keystone is down.
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(name = "org.springframework.security.oauth2.jwt.JwtDecoder")
@EnableConfigurationProperties(MeridianSecurityProperties.class)
public class ResourceServerAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "meridian.security", name = "enabled", havingValue = "true",
        matchIfMissing = true)
    public JwtDecoder meridianJwtDecoder(MeridianSecurityProperties props) {
        return NimbusJwtDecoder.withJwkSetUri(props.getJwksUri()).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "meridian.security", name = "enabled", havingValue = "true",
        matchIfMissing = true)
    public SecurityFilterChain meridianResourceServerChain(HttpSecurity http,
                                                           MeridianSecurityProperties props)
        throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authorizeHttpRequests(auth -> auth
                .antMatchers(props.getPermitAll().toArray(new String[0])).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer().jwt();
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "meridian.security", name = "enabled", havingValue = "false")
    public SecurityFilterChain meridianPermitAllChain(HttpSecurity http) throws Exception {
        http.csrf().disable().authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
