package com.meridian.platform.beacon.preference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What alerts-preferences-service returns for GET /preferences/v1/customers/{id}. */
public class CustomerPreferences {

    public static class AlertPreference {
        private String alertCode;
        private boolean enabled;
        private List<String> channels;
        private Long thresholdMinor;
        private boolean regulatory;
        private String quietHoursStart;
        private String quietHoursEnd;

        public String getAlertCode() {
            return alertCode;
        }

        public void setAlertCode(String alertCode) {
            this.alertCode = alertCode;
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

        public boolean isRegulatory() {
            return regulatory;
        }

        public void setRegulatory(boolean regulatory) {
            this.regulatory = regulatory;
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
    }

    private String customerId;
    private String locale = "en-US";
    private String timezone = "America/New_York";
    private Map<String, AlertPreference> alerts = new LinkedHashMap<>();

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Map<String, AlertPreference> getAlerts() {
        return alerts;
    }

    public void setAlerts(Map<String, AlertPreference> alerts) {
        this.alerts = alerts;
    }
}
