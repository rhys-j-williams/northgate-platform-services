package com.meridian.platform.alertprefs.publish;

import java.time.Instant;
import java.util.List;

/** Wire shape on alerts.preferences.changed.v1. Consumed by Beacon (cache evict) and audit-trail. */
public class PreferenceChangedEvent {

    private String customerId;
    private String alertCode;
    private boolean regulatory;
    private boolean enabled;
    private List<String> channels;
    private Long thresholdMinor;
    private String changedBy;
    private String correlationId;
    private Instant changedAt = Instant.now();

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAlertCode() {
        return alertCode;
    }

    public void setAlertCode(String alertCode) {
        this.alertCode = alertCode;
    }

    public boolean isRegulatory() {
        return regulatory;
    }

    public void setRegulatory(boolean regulatory) {
        this.regulatory = regulatory;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public Long getThresholdMinor() {
        return thresholdMinor;
    }

    public void setThresholdMinor(Long thresholdMinor) {
        this.thresholdMinor = thresholdMinor;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
}
