package com.meridian.platform.bedrockadapter.copybook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RecordLayoutTest {

    @Test
    void accountRecordIs136BytesAndRoundTrips() {
        AccountRecord a = new AccountRecord();
        a.setAccountId("ACC-821606081");
        a.setCustomerId("CUS-100000");
        a.setType("CREDIT-CARD");
        a.setAccountNumber("2202248504");
        a.setRoutingNumber("021000000");
        a.setCurrentBalanceMinor(-275969);
        a.setAvailableBalanceMinor(-275969);
        a.setOpenedDate(LocalDate.of(2019, 9, 4));
        a.setStatus("OPEN");
        a.setOwnerName("Delphine Castellar");

        String encoded = a.encode();
        assertThat(encoded).hasSize(Copybook.MTBACCT_LENGTH);
        assertThat(encoded.substring(67, 80)).isEqualTo("000000027596R");

        AccountRecord back = AccountRecord.decode(encoded);
        assertThat(back.getAccountId()).isEqualTo("ACC-821606081");
        assertThat(back.getRoutingNumber()).isEqualTo("021000000");
        assertThat(back.getCurrentBalanceMinor()).isEqualTo(-275969);
        assertThat(back.getOpenedDate()).isEqualTo(LocalDate.of(2019, 9, 4));
    }

    // TODO PLAT-2231 TransactionRecord and CustomerRecord have no layout tests. Offsets were
    // checked by hand against the copybook in Feb 2023 and have not moved since.
}
