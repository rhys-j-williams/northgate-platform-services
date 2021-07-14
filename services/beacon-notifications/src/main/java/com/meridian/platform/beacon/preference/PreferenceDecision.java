package com.meridian.platform.beacon.preference;

import java.util.Collections;
import java.util.List;

public final class PreferenceDecision {

    private final boolean notify;
    private final List<String> channels;
    private final String locale;
    private final String reason;

    private PreferenceDecision(boolean notify, List<String> channels, String locale, String reason) {
        this.notify = notify;
        this.channels = channels;
        this.locale = locale;
        this.reason = reason;
    }

    public static PreferenceDecision notify(List<String> channels, String locale) {
        return new PreferenceDecision(true, channels, locale, "preferences allow");
    }

    public static PreferenceDecision suppress(String reason) {
        return new PreferenceDecision(false, Collections.emptyList(), "en-US", reason);
    }

    public boolean shouldNotify() {
        return notify;
    }

    public List<String> channels() {
        return channels;
    }

    public String locale() {
        return locale;
    }

    public String reason() {
        return reason;
    }
}
