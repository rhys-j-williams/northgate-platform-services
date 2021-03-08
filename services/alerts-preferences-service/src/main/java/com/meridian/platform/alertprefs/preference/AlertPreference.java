package com.meridian.platform.alertprefs.preference;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "ALERT_PREFERENCE")
public class AlertPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_pref_seq")
    @SequenceGenerator(name = "alert_pref_seq", sequenceName = "ALERT_PREF_SEQ", allocationSize = 50)
    @Column(name = "PREFERENCE_ID")
    private Long preferenceId;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 16)
    private String customerId;

    @Column(name = "ALERT_CODE", nullable = false, length = 40)
    private String alertCode;

    @Column(name = "LABEL", nullable = false, length = 80)
    private String label;

    @Column(name = "REGULATORY", nullable = false)
    private boolean regulatory;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled = true;

    /** Comma separated, upper case. A join table was rejected in review as overkill for max 5 values. */
    @Column(name = "CHANNELS", nullable = false, length = 120)
    private String channels = "EMAIL";

    @Column(name = "THRESHOLD_MINOR")
    private Long thresholdMinor;

    @Column(name = "QUIET_HOURS_START", length = 5)
    private String quietHoursStart;

    @Column(name = "QUIET_HOURS_END", length = 5)
    private String quietHoursEnd;

    @Version
    @Column(name = "VERSION_NO", nullable = false)
    private int versionNo;

    @Column(name = "UPDATED_BY", length = 64)
    private String updatedBy;

    @Column(name = "UPDATED_AT", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getPreferenceId() {
        return preferenceId;
    }

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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
        if (channels == null || channels.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(channels.split(","));
    }

    public void setChannels(List<String> list) {
        this.channels = list.stream().map(String::toUpperCase).distinct().collect(Collectors.joining(","));
    }

    public Long getThresholdMinor() {
        return thresholdMinor;
    }

    public void setThresholdMinor(Long thresholdMinor) {
        this.thresholdMinor = thresholdMinor;
    }

    public String getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(String quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public String getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(String quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
