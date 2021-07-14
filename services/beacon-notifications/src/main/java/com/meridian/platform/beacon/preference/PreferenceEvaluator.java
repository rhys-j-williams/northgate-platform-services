package com.meridian.platform.beacon.preference;

import com.meridian.platform.beacon.event.AccountEvent;
import com.meridian.platform.beacon.event.EventType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Decides whether an event becomes a notification and on which channels.
 *
 * <p>Regulatory events always notify, on at least one durable channel (EMAIL or LETTER), whatever
 * the customer set. That rule lives in three places (here, alerts-preferences-service, and the
 * retail-web preferences screen) and they have drifted before: PLAT-1188.
 */
@Component
public class PreferenceEvaluator {

    static final List<String> REGULATORY_MINIMUM = Collections.singletonList("EMAIL");
    static final List<String> DURABLE = Arrays.asList("EMAIL", "LETTER");

    private final PreferencesClient client;

    public PreferenceEvaluator(PreferencesClient client) {
        this.client = client;
    }

    public PreferenceDecision evaluate(AccountEvent event) {
        CustomerPreferences prefs = client.forCustomer(event.getCustomerId());
        CustomerPreferences.AlertPreference pref = prefs.getAlerts().get(event.getEventType().alertCode());

        if (event.getEventType().isRegulatory()) {
            List<String> channels = pref == null ? REGULATORY_MINIMUM : pref.getChannels();
            if (channels.stream().noneMatch(DURABLE::contains)) {
                channels = REGULATORY_MINIMUM;
            }
            return PreferenceDecision.notify(channels, prefs.getLocale());
        }
        if (pref == null) {
            return PreferenceDecision.suppress("no preference for " + event.getEventType().alertCode());
        }
        if (!pref.isEnabled()) {
            return PreferenceDecision.suppress("alert disabled by customer");
        }
        if (pref.getThresholdMinor() != null) {
            // Balance alerts threshold on the balance after the event, everything else on the amount.
            if (event.getEventType() == EventType.LOW_BALANCE) {
                if (event.getBalanceAfterMinor() > pref.getThresholdMinor()) {
                    return PreferenceDecision.suppress("balance above threshold " + pref.getThresholdMinor());
                }
            } else if (Math.abs(event.getAmountMinor()) < pref.getThresholdMinor()) {
                return PreferenceDecision.suppress("below threshold " + pref.getThresholdMinor());
            }
        }
        if (pref.getChannels() == null || pref.getChannels().isEmpty()) {
            return PreferenceDecision.suppress("no channels selected");
        }
        return PreferenceDecision.notify(pref.getChannels(), prefs.getLocale());
    }
}
