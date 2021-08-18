package com.meridian.platform.piivault.keys;

import com.fasterxml.jackson.databind.JsonNode;
import com.meridian.platform.common.correlation.CorrelationId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Fetches FPE key material from Vault (transit engine, key name pii-fpe). Locally that is the Vault
 * mock on 4605, which returns a fixed key per version. If Vault is unreachable we fall back to a
 * derived dev key so the service still starts; that fallback is refused when the prod profile is
 * active (see {@code meridian.vault.allow-dev-key}). GIS signed this off for non-prod only,
 * GIS-0522.
 */
@Component
public class VaultKeyClient {

    private static final Logger log = LoggerFactory.getLogger(VaultKeyClient.class);

    private final RestTemplate http;
    private final String baseUrl;
    private final String token;
    private final String keyName;
    private final boolean allowDevKey;
    private final Map<Integer, KeyMaterial> cache = new ConcurrentHashMap<>();
    private volatile int currentVersion = 1;

    public VaultKeyClient(RestTemplateBuilder builder,
                          @Value("${meridian.vault.url:http://localhost:4605}") String baseUrl,
                          @Value("${meridian.vault.token:CHANGEME-vault-token}") String token,
                          @Value("${meridian.vault.key-name:pii-fpe}") String keyName,
                          @Value("${meridian.vault.allow-dev-key:true}") boolean allowDevKey) {
        this.http = builder.setConnectTimeout(Duration.ofMillis(800)).setReadTimeout(Duration.ofSeconds(2)).build();
        this.baseUrl = baseUrl;
        this.token = token;
        this.keyName = keyName;
        this.allowDevKey = allowDevKey;
    }

    public KeyMaterial current() {
        return forVersion(currentVersion);
    }

    public KeyMaterial forVersion(int version) {
        return cache.computeIfAbsent(version, this::fetch);
    }

    private KeyMaterial fetch(int version) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Vault-Token", token);
            headers.set(CorrelationId.HEADER, CorrelationId.current());
            JsonNode body = http.exchange(baseUrl + "/v1/transit/export/encryption-key/" + keyName + "/" + version,
                HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
            String b64 = body == null ? null : body.path("data").path("keys").path(String.valueOf(version)).asText(null);
            if (b64 != null) {
                JsonNode latest = body.path("data").path("latest_version");
                if (latest.isInt() && latest.asInt() > currentVersion) {
                    currentVersion = latest.asInt();
                }
                return new KeyMaterial(version, Base64.getDecoder().decode(b64), "vault");
            }
            log.warn("vault returned no key material for {} v{}", keyName, version);
        } catch (RestClientException e) {
            log.warn("vault unreachable at {}: {}", baseUrl, e.getMessage());
        }
        if (!allowDevKey) {
            throw new IllegalStateException("Vault unavailable and dev key not permitted in this profile");
        }
        return new KeyMaterial(version, devKey(version), "dev-derived");
    }

    private static byte[] devKey(int version) {
        try {
            // Not a secret. Deterministic so tokens survive a restart in local/demo.
            return MessageDigest.getInstance("SHA-256")
                .digest(("meridian-pii-dev-key-v" + version).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
