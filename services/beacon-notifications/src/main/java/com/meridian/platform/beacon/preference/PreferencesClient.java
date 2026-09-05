package com.meridian.platform.beacon.preference;

import com.fasterxml.jackson.databind.JsonNode;
import com.meridian.platform.common.correlation.CorrelationId;
import com.meridian.platform.common.fixtures.FixtureStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * Reads preferences from alerts-preferences-service, falling back to the fixture set when it is
 * unreachable. The fallback exists so Beacon can be demonstrated stand-alone; in the bank a
 * preferences outage means no non-regulatory alerts go out, which is the documented behaviour.
 */
@Component
public class PreferencesClient {

    private static final Logger log = LoggerFactory.getLogger(PreferencesClient.class);
    private static final List<String> DEFAULT_CHANNELS = Arrays.asList("PUSH", "EMAIL");

    private final RestTemplate http;
    private final FixtureStore fixtures;
    private final String baseUrl;
    private final boolean fixtureFallback;
    private final Map<String, CustomerPreferences> cache = new ConcurrentHashMap<>();

    public PreferencesClient(RestTemplateBuilder builder, FixtureStore fixtures,
                             @Value("${meridian.beacon.preferences-url:http://localhost:4511/preferences/v1}") String baseUrl,
                             @Value("${meridian.beacon.fixture-fallback:true}") boolean fixtureFallback) {
        this.http = builder.setConnectTimeout(Duration.ofMillis(800)).setReadTimeout(Duration.ofSeconds(2)).build();
        this.fixtures = fixtures;
        this.baseUrl = baseUrl;
        this.fixtureFallback = fixtureFallback;
    }

    public CustomerPreferences forCustomer(String customerId) {
        CustomerPreferences cached = cache.get(customerId);
        if (cached != null) {
            return cached;
        }
        CustomerPreferences result;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(CorrelationId.HEADER, CorrelationId.current());
            result = http.exchange(baseUrl + "/customers/" + customerId, HttpMethod.GET,
                new HttpEntity<>(headers), CustomerPreferences.class).getBody();
        } catch (RestClientException e) {
            if (!fixtureFallback) {
                throw e;
            }
            log.warn("alerts-preferences-service unreachable ({}), using fixture preferences for {}",
                e.getClass().getSimpleName(), customerId);
            result = fromFixtures(customerId);
        }
        if (result == null) {
            result = fromFixtures(customerId);
        }
        cache.put(customerId, result);
        return result;
    }

    public void evict(String customerId) {
        cache.remove(customerId);
    }

    CustomerPreferences fromFixtures(String customerId) {
        CustomerPreferences prefs = new CustomerPreferences();
        prefs.setCustomerId(customerId);
        for (JsonNode node : fixtures.alertPreferencesFor(customerId)) {
            CustomerPreferences.AlertPreference p = new CustomerPreferences.AlertPreference();
            p.setAlertCode(node.path("alertCode").asText());
            p.setEnabled(node.path("enabled").asBoolean(true));
            p.setRegulatory(node.path("regulatory").asBoolean(false));
            List<String> channels = new ArrayList<>();
            node.path("channels").forEach(c -> channels.add(c.asText().toUpperCase()));
            p.setChannels(channels.isEmpty() ? DEFAULT_CHANNELS : channels);
            if (node.hasNonNull("thresholdMinor")) {
                p.setThresholdMinor(node.get("thresholdMinor").asLong());
            }
            prefs.getAlerts().put(p.getAlertCode(), p);
        }
        return prefs;
    }
}
