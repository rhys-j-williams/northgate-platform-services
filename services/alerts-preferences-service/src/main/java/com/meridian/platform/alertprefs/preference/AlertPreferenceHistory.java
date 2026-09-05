package com.meridian.platform.alertprefs.preference;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "ALERT_PREFERENCE_HIST")
public class AlertPreferenceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_pref_hist_seq")
    @SequenceGenerator(name = "alert_pref_hist_seq", sequenceName = "ALERT_PREF_HIST_SEQ", allocationSize = 50)
    @Column(name = "HIST_ID")
    private Long histId;

    @Column(name = "PREFERENCE_ID", nullable = false)
    private Long preferenceId;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 16)
    private String customerId;

    @Column(name = "ALERT_CODE", nullable = false, length = 40)
    private String alertCode;

    @Column(name = "ENABLED_BEFORE")
    private Boolean enabledBefore;

    @Column(name = "ENABLED_AFTER")
    private Boolean enabledAfter;

    @Column(name = "CHANNELS_BEFORE", length = 120)
    private String channelsBefore;

    @Column(name = "CHANNELS_AFTER", length = 120)
    private String channelsAfter;

    @Column(name = "CHANGED_BY", length = 64)
    private String changedBy;

    @Column(name = "CORRELATION_ID", length = 64)
    private String correlationId;

    @Column(name = "CHANGED_AT", nullable = false)
    private Instant changedAt = Instant.now();

    public static AlertPreferenceHistory of(AlertPreference before, AlertPreference after, String changedBy, String correlationId) {
        AlertPreferenceHistory h = new AlertPreferenceHistory();
        h.preferenceId = after.getPreferenceId();
        h.customerId = after.getCustomerId();
        h.alertCode = after.getAlertCode();
        h.enabledBefore = before == null ? null : before.isEnabled();
        h.enabledAfter = after.isEnabled();
        h.channelsBefore = before == null ? null : String.join(",", before.getChannels());
        h.channelsAfter = String.join(",", after.getChannels());
        h.changedBy = changedBy;
        h.correlationId = correlationId;
        return h;
    }

    public Long getHistId() {
        return histId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getAlertCode() {
        return alertCode;
    }

    public Boolean getEnabledBefore() {
        return enabledBefore;
    }

    public Boolean getEnabledAfter() {
        return enabledAfter;
    }

    public String getChannelsBefore() {
        return channelsBefore;
    }

    public String getChannelsAfter() {
        return channelsAfter;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
