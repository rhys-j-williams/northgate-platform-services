package com.meridian.platform.bedrockadapter.copybook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ZonedDecimalTest {

    // Examples from copybooks/README.md. Same table drives bedrock.spec.ts in domain-fixtures.
    @ParameterizedTest
    @CsvSource({
        "1234, 10, 000000123D",
        "-1234, 10, 000000123M",
        "0, 10, 000000000{",
        "-10, 10, 000000001}",
        "1250000, 13, 000000125000{",
        "-99, 3, 09R"
    })
    void encodesLikeBedrock(long minor, int width, String expected) {
        assertThat(ZonedDecimal.encode(minor, width)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "000000123D, 1234",
        "000000123M, -1234",
        "000000000{, 0",
        "000000001}, -10",
        "000000125000{, 1250000"
    })
    void decodesLikeBedrock(String field, long expected) {
        assertThat(ZonedDecimal.decode(field)).isEqualTo(expected);
    }

    @Test
    void roundTripsAcrossTheSignBoundary() {
        for (long v = -105; v <= 105; v++) {
            assertThat(ZonedDecimal.decode(ZonedDecimal.encode(v, 13))).isEqualTo(v);
        }
    }

    @Test
    void refusesPlainDigitsInSignPosition() {
        // A field that was written by something that forgot the overpunch. INC0048812 again.
        assertThatThrownBy(() -> ZonedDecimal.decode("0000001234"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesOverflow() {
        assertThatThrownBy(() -> ZonedDecimal.encode(12345, 4)).isInstanceOf(IllegalArgumentException.class);
    }
}
