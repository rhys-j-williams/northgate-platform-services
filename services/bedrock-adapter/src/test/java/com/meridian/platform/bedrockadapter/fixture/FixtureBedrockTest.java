package com.meridian.platform.bedrockadapter.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import com.meridian.platform.bedrockadapter.copybook.AccountRecord;
import com.meridian.platform.common.fixtures.FixtureStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Reads the real fixture export, so this doubles as a parity check between the TypeScript
 * encoder in domain-fixtures and the Java decoders here.
 */
class FixtureBedrockTest {

    private static FixtureStore fixtures;

    @BeforeAll
    static void load() {
        fixtures = FixtureStore.load();
        assertThat(fixtures.isEmpty()).as("fixtures/meridian-fixtures.json must exist, run make install").isFalse();
    }

    @Test
    void everyFixtureAccountRecordDecodesAndCarriesTheTestRoutingNumber() {
        for (String raw : fixtures.bedrockAccountRecords()) {
            AccountRecord a = AccountRecord.decode(raw);
            assertThat(a.getRoutingNumber()).isEqualTo("021000000");
            assertThat(a.getAccountId()).startsWith("ACC-");
        }
    }

    // TODO PLAT-2231: no tests for MTAI/MTTP/MTCP request handling, RC mapping or the
    // idempotency echo. Covered manually against the CICS test region before each release.
}
