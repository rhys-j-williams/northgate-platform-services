package com.meridian.platform.alertprefs.preference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.meridian.platform.common.error.ApiException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class RegulatoryGuardTest {

    private AlertPreference regulatory() {
        AlertPreference p = new AlertPreference();
        p.setAlertCode("OVERDRAFT_NOTICE");
        p.setRegulatory(true);
        p.setChannels(Arrays.asList("EMAIL", "IN-APP"));
        return p;
    }

    private AlertPreference optional() {
        AlertPreference p = new AlertPreference();
        p.setAlertCode("BALANCE_LOW");
        p.setChannels(Arrays.asList("EMAIL", "SMS"));
        return p;
    }

    @Test
    void regulatoryCannotBeDisabled() {
        PreferenceUpdate u = new PreferenceUpdate();
        u.setEnabled(false);
        assertThatThrownBy(() -> RegulatoryGuard.apply(regulatory(), u))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("REGULATORY_LOCKED"));
    }

    @Test
    void regulatoryMustKeepDurableChannel() {
        PreferenceUpdate u = new PreferenceUpdate();
        u.setChannels(Arrays.asList("sms", "push"));
        assertThatThrownBy(() -> RegulatoryGuard.apply(regulatory(), u))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("REGULATORY_CHANNEL"));
    }

    @Test
    void regulatoryChannelsCanChangeIfDurableRemains() {
        PreferenceUpdate u = new PreferenceUpdate();
        u.setChannels(Arrays.asList("sms", "letter"));
        assertThat(RegulatoryGuard.apply(regulatory(), u)).containsExactly("SMS", "LETTER");
    }

    @Test
    void optionalAlertCanBeDisabledAndMoved() {
        PreferenceUpdate u = new PreferenceUpdate();
        u.setEnabled(false);
        u.setChannels(Collections.singletonList("push"));
        assertThat(RegulatoryGuard.apply(optional(), u)).containsExactly("PUSH");
    }

    @Test
    void unknownChannelRejected() {
        PreferenceUpdate u = new PreferenceUpdate();
        u.setChannels(Arrays.asList("EMAIL", "fax"));
        assertThatThrownBy(() -> RegulatoryGuard.apply(optional(), u))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("UNKNOWN_CHANNEL"));
    }

    @Test
    void emptyChannelListRejected() {
        PreferenceUpdate u = new PreferenceUpdate();
        u.setChannels(Collections.emptyList());
        assertThatThrownBy(() -> RegulatoryGuard.apply(optional(), u)).isInstanceOf(ApiException.class);
    }

    @Test
    void channelsDeduplicatedAndUpperCased() {
        PreferenceUpdate u = new PreferenceUpdate();
        u.setChannels(Arrays.asList("email", "Email", "SMS"));
        assertThat(RegulatoryGuard.apply(optional(), u)).containsExactly("EMAIL", "SMS");
    }
}
