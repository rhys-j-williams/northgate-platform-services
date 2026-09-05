package com.meridian.platform.alertprefs.preference;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AlertCatalogueTest {

    @Test
    void regulatoryCodesMatchComplianceList() {
        // Reg DD, Reg E, GLBA, TILA. If this list changes, Compliance signed it off first.
        assertThat(AlertCatalogue.entries().values().stream().filter(e -> e.regulatory).map(e -> e.code))
            .containsExactlyInAnyOrder("OVERDRAFT_NOTICE", "REG_E_ERROR_RESOLUTION", "PRIVACY_NOTICE", "RATE_CHANGE");
    }

    @Test
    void regulatoryDefaultsIncludeDurableChannel() {
        AlertCatalogue.entries().values().stream().filter(e -> e.regulatory)
            .forEach(e -> assertThat(e.defaultChannels).containsAnyOf("EMAIL", "LETTER", "IN-APP"));
    }

    @Test
    void unknownCodeIsNotRegulatory() {
        assertThat(AlertCatalogue.isRegulatory("MADE_UP")).isFalse();
        assertThat(AlertCatalogue.get("MADE_UP")).isNull();
    }

    @Test
    void channelCheckIsCaseInsensitive() {
        assertThat(AlertCatalogue.isKnownChannel("email")).isTrue();
        assertThat(AlertCatalogue.isKnownChannel("IN-APP")).isTrue();
        assertThat(AlertCatalogue.isKnownChannel(null)).isFalse();
    }
}
