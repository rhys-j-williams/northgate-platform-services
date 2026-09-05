package com.meridian.platform.alertprefs.preference;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The alert codes a customer can hold a preference for, and which of them are regulatory. The
 * regulatory flag here is the source of truth for the "cannot be disabled" rule; the fixture flag
 * and the database column are copies of it. If Compliance add a notice, it goes here first
 * (PLAT-0603 has the process).
 */
public final class AlertCatalogue {

    public static final class Entry {
        public final String code;
        public final String label;
        public final boolean regulatory;
        public final List<String> defaultChannels;

        Entry(String code, String label, boolean regulatory, String... channels) {
            this.code = code;
            this.label = label;
            this.regulatory = regulatory;
            this.defaultChannels = Collections.unmodifiableList(Arrays.asList(channels));
        }
    }

    public static final Set<String> CHANNELS = Collections.unmodifiableSet(
        new TreeSet<>(Arrays.asList("EMAIL", "SMS", "PUSH", "IN-APP", "LETTER")));

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        add("BALANCE_LOW", "Low balance", false, "EMAIL", "PUSH");
        add("LARGE_TRANSACTION", "Large transaction", false, "EMAIL", "SMS");
        add("CARD_DECLINED", "Card declined", false, "PUSH", "SMS");
        add("DEPOSIT_POSTED", "Deposit posted", false, "PUSH");
        add("PAYLINK_RECEIVED", "PayLink payment received", false, "PUSH", "EMAIL");
        add("SECURITY_SIGN_IN", "New device sign in", false, "PUSH", "EMAIL", "SMS");
        // Regulatory. Reg DD / Reg E / GLBA. Always enabled, always at least one durable channel.
        add("OVERDRAFT_NOTICE", "Overdraft notice", true, "EMAIL", "IN-APP");
        add("REG_E_ERROR_RESOLUTION", "Error resolution outcome", true, "EMAIL", "IN-APP");
        add("PRIVACY_NOTICE", "Annual privacy notice", true, "EMAIL", "IN-APP");
        add("RATE_CHANGE", "Rate or fee change", true, "EMAIL", "IN-APP");
    }

    private static void add(String code, String label, boolean regulatory, String... channels) {
        ENTRIES.put(code, new Entry(code, label, regulatory, channels));
    }

    private AlertCatalogue() {
    }

    public static Map<String, Entry> entries() {
        return Collections.unmodifiableMap(ENTRIES);
    }

    public static Entry get(String code) {
        return ENTRIES.get(code);
    }

    public static boolean isRegulatory(String code) {
        Entry e = ENTRIES.get(code);
        return e != null && e.regulatory;
    }

    public static boolean isKnownChannel(String channel) {
        return channel != null && CHANNELS.contains(channel.toUpperCase());
    }
}
