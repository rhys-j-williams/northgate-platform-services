package com.meridian.platform.beacon.admin;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Admin endpoints authenticate against corporate AD in the bank (ldaps://ad.corp, group
 * CN=APP-BEACON-OPS). Locally an UnboundID in-memory directory is seeded from
 * beacon-ldap.ldif with the same DIT so the bind and group filters are identical.
 *
 * <p>Ordered before the common-starter resource server chain, which handles everything else.
 */
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
public class AdminSecurityConfig {

    @Bean(destroyMethod = "shutDown")
    public InMemoryDirectoryServer embeddedLdap(@Value("${meridian.ldap.embedded-port:0}") int port)
        throws LDAPException, IOException {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=meridian,dc=local");
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", port));
        config.setSchema(null);
        InMemoryDirectoryServer server = new InMemoryDirectoryServer(config);
        try (InputStream in = new ClassPathResource("beacon-ldap.ldif").getInputStream()) {
            server.importFromLDIF(true, new com.unboundid.ldif.LDIFReader(in));
        }
        server.startListening();
        return server;
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(InMemoryDirectoryServer ldap) {
        String url = "ldap://localhost:" + ldap.getListenPort() + "/dc=meridian,dc=local";
        DefaultSpringSecurityContextSource source = new DefaultSpringSecurityContextSource(url);
        source.afterPropertiesSet();
        BindAuthenticator bind = new BindAuthenticator(source);
        bind.setUserSearch(new FilterBasedLdapUserSearch("ou=people", "(uid={0})", source));
        DefaultLdapAuthoritiesPopulator groups = new DefaultLdapAuthoritiesPopulator(source, "ou=groups");
        groups.setGroupSearchFilter("(member={0})");
        groups.setGroupRoleAttribute("cn");
        groups.setRolePrefix("ROLE_");
        groups.setConvertToUpperCase(true);
        return new LdapAuthenticationProvider(bind, groups);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminChain(HttpSecurity http, LdapAuthenticationProvider ldap) throws Exception {
        http.antMatcher("/beacon/v1/admin/**")
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authenticationProvider(ldap)
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("BEACON-OPS"))
            .httpBasic();
        return http.build();
    }

    static String utf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
