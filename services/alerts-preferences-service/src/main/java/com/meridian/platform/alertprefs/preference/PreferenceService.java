package com.meridian.platform.alertprefs.preference;

import com.fasterxml.jackson.databind.JsonNode;
import com.meridian.platform.alertprefs.publish.PreferenceChangedEvent;
import com.meridian.platform.alertprefs.publish.PreferencePublisher;
import com.meridian.platform.common.correlation.CorrelationId;
import com.meridian.platform.common.error.ApiException;
import com.meridian.platform.common.fixtures.FixtureStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceService {

    private static final Logger log = LoggerFactory.getLogger(PreferenceService.class);

    private final AlertPreferenceRepository repository;
    private final AlertPreferenceHistoryRepository history;
    private final PreferencePublisher publisher;
    private final FixtureStore fixtures;

    public PreferenceService(AlertPreferenceRepository repository, AlertPreferenceHistoryRepository history,
                             PreferencePublisher publisher, FixtureStore fixtures) {
        this.repository = repository;
        this.history = history;
        this.publisher = publisher;
        this.fixtures = fixtures;
    }

    /**
     * A customer with no rows gets the catalogue defaults, seeded from the fixture set where it
     * has an opinion. In the bank the seed happens at enrolment (KEY-2210 webhook); here it is
     * lazy so the demo works from an empty H2.
     */
    @Transactional
    public List<AlertPreference> forCustomer(String customerId) {
        List<AlertPreference> existing = repository.findByCustomerIdOrderByAlertCodeAsc(customerId);
        if (!existing.isEmpty()) {
            return existing;
        }
        return seed(customerId);
    }

    @Transactional
    public AlertPreference get(String customerId, String alertCode) {
        forCustomer(customerId);
        return repository.findByCustomerIdAndAlertCode(customerId, alertCode)
            .orElseThrow(() -> ApiException.notFound("PREFERENCE_NOT_FOUND", "no preference " + alertCode + " for " + customerId));
    }

    @Transactional
    public AlertPreference update(String customerId, String alertCode, PreferenceUpdate update, String changedBy) {
        AlertPreference current = get(customerId, alertCode);
        AlertPreference before = snapshot(current);

        List<String> channels = RegulatoryGuard.apply(current, update);
        current.setChannels(channels);
        if (update.getEnabled() != null) {
            current.setEnabled(update.getEnabled());
        }
        if (update.getThresholdMinor() != null) {
            current.setThresholdMinor(update.getThresholdMinor());
        }
        if (update.getQuietHoursStart() != null) {
            current.setQuietHoursStart(update.getQuietHoursStart());
        }
        if (update.getQuietHoursEnd() != null) {
            current.setQuietHoursEnd(update.getQuietHoursEnd());
        }
        current.setUpdatedBy(changedBy);
        current.setUpdatedAt(Instant.now());
        AlertPreference saved = repository.save(current);

        String correlationId = CorrelationId.current();
        history.save(AlertPreferenceHistory.of(before, saved, changedBy, correlationId));
        publisher.publish(toEvent(saved, changedBy, correlationId));
        log.info("preference updated customer={} code={} enabled={} channels={}", customerId, alertCode,
            saved.isEnabled(), saved.getChannels());
        return saved;
    }

    public List<AlertPreferenceHistory> historyFor(String customerId) {
        return history.findByCustomerIdOrderByChangedAtDesc(customerId);
    }

    List<AlertPreference> seed(String customerId) {
        List<JsonNode> fixtureRows = fixtures.alertPreferencesFor(customerId);
        List<AlertPreference> out = new ArrayList<>();
        for (AlertCatalogue.Entry entry : AlertCatalogue.entries().values()) {
            AlertPreference p = new AlertPreference();
            p.setCustomerId(customerId);
            p.setAlertCode(entry.code);
            p.setLabel(entry.label);
            p.setRegulatory(entry.regulatory);
            p.setEnabled(true);
            p.setChannels(entry.defaultChannels);
            p.setUpdatedBy("seed");
            JsonNode fixture = fixtureRows.stream().filter(n -> entry.code.equals(n.path("code").asText())).findFirst().orElse(null);
            if (fixture != null) {
                if (!entry.regulatory) {
                    p.setEnabled(fixture.path("enabled").asBoolean(true));
                }
                List<String> channels = new ArrayList<>();
                fixture.path("channels").forEach(c -> channels.add(c.asText().toUpperCase()));
                if (!channels.isEmpty()) {
                    p.setChannels(channels);
                }
                if (fixture.hasNonNull("thresholdMinor")) {
                    p.setThresholdMinor(fixture.get("thresholdMinor").asLong());
                }
            }
            out.add(repository.save(p));
        }
        log.info("seeded {} preferences for customer={} fromFixtures={}", out.size(), customerId, !fixtureRows.isEmpty());
        return out;
    }

    private static AlertPreference snapshot(AlertPreference p) {
        AlertPreference s = new AlertPreference();
        s.setCustomerId(p.getCustomerId());
        s.setAlertCode(p.getAlertCode());
        s.setEnabled(p.isEnabled());
        s.setRegulatory(p.isRegulatory());
        s.setChannels(p.getChannels());
        return s;
    }

    private static PreferenceChangedEvent toEvent(AlertPreference p, String changedBy, String correlationId) {
        PreferenceChangedEvent e = new PreferenceChangedEvent();
        e.setCustomerId(p.getCustomerId());
        e.setAlertCode(p.getAlertCode());
        e.setRegulatory(p.isRegulatory());
        e.setEnabled(p.isEnabled());
        e.setChannels(p.getChannels());
        e.setThresholdMinor(p.getThresholdMinor());
        e.setChangedBy(changedBy);
        e.setCorrelationId(correlationId);
        return e;
    }
}
