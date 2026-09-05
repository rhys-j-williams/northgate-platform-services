package com.meridian.platform.alertprefs.api;

import com.meridian.platform.alertprefs.preference.AlertCatalogue;
import com.meridian.platform.alertprefs.preference.AlertPreference;
import com.meridian.platform.alertprefs.preference.AlertPreferenceHistory;
import com.meridian.platform.alertprefs.preference.PreferenceService;
import com.meridian.platform.alertprefs.preference.PreferenceUpdate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/preferences/v1")
public class PreferencesController {

    private final PreferenceService service;

    public PreferencesController(PreferenceService service) {
        this.service = service;
    }

    /** Shape consumed by Beacon's PreferencesClient: keep {@code alerts} keyed by code. */
    @GetMapping("/customers/{customerId}")
    public Map<String, Object> forCustomer(@PathVariable String customerId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("customerId", customerId);
        out.put("locale", "en-US");
        out.put("timezone", "America/New_York");
        Map<String, Object> alerts = new LinkedHashMap<>();
        for (AlertPreference p : service.forCustomer(customerId)) {
            alerts.put(p.getAlertCode(), view(p));
        }
        out.put("alerts", alerts);
        return out;
    }

    @GetMapping("/customers/{customerId}/alerts/{alertCode}")
    public Map<String, Object> one(@PathVariable String customerId, @PathVariable String alertCode) {
        return view(service.get(customerId, alertCode.toUpperCase()));
    }

    @PutMapping("/customers/{customerId}/alerts/{alertCode}")
    public Map<String, Object> update(@PathVariable String customerId, @PathVariable String alertCode,
                                      @Valid @RequestBody PreferenceUpdate update, Authentication auth) {
        String actor = auth == null ? "anonymous" : auth.getName();
        return view(service.update(customerId, alertCode.toUpperCase(), update, actor));
    }

    @GetMapping("/customers/{customerId}/history")
    public List<Map<String, Object>> history(@PathVariable String customerId) {
        return service.historyFor(customerId).stream().map(PreferencesController::view).collect(Collectors.toList());
    }

    @GetMapping("/catalogue")
    public List<Map<String, Object>> catalogue() {
        return AlertCatalogue.entries().values().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", e.code);
            m.put("label", e.label);
            m.put("regulatory", e.regulatory);
            m.put("defaultChannels", e.defaultChannels);
            return m;
        }).collect(Collectors.toList());
    }

    static Map<String, Object> view(AlertPreference p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("alertCode", p.getAlertCode());
        m.put("label", p.getLabel());
        m.put("regulatory", p.isRegulatory());
        m.put("enabled", p.isEnabled());
        m.put("channels", p.getChannels());
        m.put("thresholdMinor", p.getThresholdMinor());
        m.put("quietHoursStart", p.getQuietHoursStart());
        m.put("quietHoursEnd", p.getQuietHoursEnd());
        m.put("version", p.getVersionNo());
        m.put("updatedAt", p.getUpdatedAt());
        return m;
    }

    static Map<String, Object> view(AlertPreferenceHistory h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("alertCode", h.getAlertCode());
        m.put("enabledBefore", h.getEnabledBefore());
        m.put("enabledAfter", h.getEnabledAfter());
        m.put("channelsBefore", h.getChannelsBefore());
        m.put("channelsAfter", h.getChannelsAfter());
        m.put("changedBy", h.getChangedBy());
        m.put("correlationId", h.getCorrelationId());
        m.put("changedAt", h.getChangedAt());
        return m;
    }
}
