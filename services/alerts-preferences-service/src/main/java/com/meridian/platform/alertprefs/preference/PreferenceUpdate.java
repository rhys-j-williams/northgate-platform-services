package com.meridian.platform.alertprefs.preference;

import java.util.List;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

/** Partial update. Nulls mean "leave alone". */
public class PreferenceUpdate {

    private Boolean enabled;
    private List<String> channels;
    @PositiveOrZero
    private Long thresholdMinor;
    @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "HH:mm")
    private String quietHoursStart;
    @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "HH:mm")
    private String quietHoursEnd;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
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
