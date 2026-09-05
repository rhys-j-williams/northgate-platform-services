package com.meridian.platform.alertprefs.preference;

import com.meridian.platform.common.error.ApiException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The rule Compliance care about. A regulatory alert cannot be disabled, and must keep at least
 * one durable channel (email, letter or in-app inbox; SMS and push are not considered durable by
 * Legal since the 2022 Reg E review). Anything else about it can change.
 */
public final class RegulatoryGuard {

    static final List<String> DURABLE = Arrays.asList("EMAIL", "LETTER", "IN-APP");

    private RegulatoryGuard() {
    }

    /** Returns the channels to persist, or throws 422 REGULATORY_LOCKED. */
    public static List<String> apply(AlertPreference current, PreferenceUpdate update) {
        if (!current.isRegulatory()) {
            return update.getChannels() == null ? current.getChannels() : normalise(update.getChannels());
        }
        if (Boolean.FALSE.equals(update.getEnabled())) {
            throw new ApiException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "REGULATORY_LOCKED",
                current.getAlertCode() + " is a regulatory notice and cannot be switched off");
        }
        List<String> channels = update.getChannels() == null ? current.getChannels() : normalise(update.getChannels());
        if (channels.stream().noneMatch(DURABLE::contains)) {
            throw new ApiException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "REGULATORY_CHANNEL",
                current.getAlertCode() + " must keep at least one of " + DURABLE);
        }
        return channels;
    }

    static List<String> normalise(List<String> channels) {
        List<String> out = new ArrayList<>();
        for (String c : channels) {
            String u = c == null ? "" : c.trim().toUpperCase();
            if (!AlertCatalogue.isKnownChannel(u)) {
                throw ApiException.badRequest("UNKNOWN_CHANNEL", "unknown channel '" + c + "'");
            }
            if (!out.contains(u)) {
                out.add(u);
            }
        }
        if (out.isEmpty()) {
            throw ApiException.badRequest("NO_CHANNELS", "at least one channel is required");
        }
        return out;
    }
}
