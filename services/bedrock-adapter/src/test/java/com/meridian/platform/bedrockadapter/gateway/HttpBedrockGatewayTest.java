package com.meridian.platform.bedrockadapter.gateway;

import com.meridian.platform.bedrockadapter.copybook.Copybook;
import com.meridian.platform.bedrockadapter.copybook.ZonedDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Envelope translation only. The round trip against bedrock-core-mock is covered by
 * mock-external/smoke.sh, not here; nobody wants an HTTP server in the unit suite (PLAT-1187).
 */
class HttpBedrockGatewayTest {

    @Test
    void mtbreqIsTwoHundredBytesWithFieldsAtCopybookOffsets() {
        String rec = HttpBedrockGateway.mtbreq("TRANPOST", "corr-1", "ACC-000000001", "", -1250L, "idem-0001", "", "RTL", "COFFEE");
        assertThat(rec).hasSize(HttpBedrockGateway.MTBREQ_LENGTH);
        assertThat(rec.substring(0, 8)).isEqualTo("TRANPOST");
        assertThat(rec.substring(8, 44).trim()).isEqualTo("corr-1");
        assertThat(rec.substring(44, 60).trim()).isEqualTo("ACC-000000001");
        assertThat(ZonedDecimal.decode(rec.substring(72, 85))).isEqualTo(-1250L);
        assertThat(rec.substring(85, 101).trim()).isEqualTo("idem-0001");
        assertThat(rec.substring(105, 113).trim()).isEqualTo("RTL");
        assertThat(rec.substring(113, 177).trim()).isEqualTo("COFFEE");
    }

    @Test
    void mtbrespHeaderParses() {
        String header = String.format("%-8s%-36s%s%-4s%s", "ACCTINQ", "corr-2", "0008", "", "0000");
        HttpBedrockGateway.Mtbresp r = HttpBedrockGateway.Mtbresp.parse(header);
        assertThat(r.func).isEqualTo("ACCTINQ");
        assertThat(r.correlationId).isEqualTo("corr-2");
        assertThat(r.returnCode).isEqualTo(8);
        assertThat(r.count).isZero();
        assertThat(r.record(0, Copybook.MTBACCT_LENGTH)).hasSize(Copybook.MTBACCT_LENGTH);
    }

    @Test
    void shortReplyIsTreatedAsUnavailable() {
        assertThatThrownBy(() -> HttpBedrockGateway.Mtbresp.parse("ACCTINQ"))
            .isInstanceOf(BedrockUnavailableException.class);
    }
}
