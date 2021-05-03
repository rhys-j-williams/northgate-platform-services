package com.meridian.platform.piivault.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiTypeTest {

    @Test
    void parsesLooseSpellings() {
        assertThat(PiiType.parse("ssn")).isEqualTo(PiiType.SSN);
        assertThat(PiiType.parse("account-number")).isEqualTo(PiiType.ACCOUNT_NUMBER);
    }

    @Test
    void maskKeepsLastFour() {
        assertThat(TokenService.mask("123-45-6789", PiiType.SSN)).isEqualTo("***-**-6789");
    }

    // FormatPreservingCipher round trip is covered by the GIS pen-test harness, not here (GIS-0522).
    // Nobody has ported it into the unit suite. PLAT-1477.
}
