package com.meridian.platform.beacon.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.meridian.platform.beacon.event.AccountEvent;
import com.meridian.platform.beacon.event.EventType;
import com.meridian.platform.beacon.preference.PreferenceDecision;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class TemplateRegistryTest {

    private final TemplateRegistry registry = new TemplateRegistry();

    @Test
    void formatsMoneyWithGroupingAndSign() {
        assertThat(TemplateRegistry.money(123456789L)).isEqualTo("$1,234,567.89");
        assertThat(TemplateRegistry.money(-5L)).isEqualTo("-$0.05");
        assertThat(TemplateRegistry.money(0L)).isEqualTo("$0.00");
    }

    @Test
    void rendersRegulatoryOverdraftNoticeWithDisclosureBlock() {
        AccountEvent e = new AccountEvent();
        e.setEventId("EVT-1");
        e.setCustomerId("CUS-100000");
        e.setAccountId("ACC-821606081");
        e.setEventType(EventType.OVERDRAFT);
        e.setAmountMinor(-4200);
        e.setBalanceAfterMinor(-1550);
        e.setDescription("Card purchase");
        e.setOccurredAt(Instant.parse("2024-03-04T15:30:00Z"));

        RenderedTemplate r = registry.render(e, PreferenceDecision.notify(Collections.singletonList("EMAIL"), "en-US"));

        assertThat(r.getTemplateCode()).isEqualTo("REG_OVERDRAFT_NOTICE");
        assertThat(r.getSubject()).isEqualTo("Overdraft notice for account ending 6081");
        assertThat(r.getBody()).contains("IMPORTANT DISCLOSURE").contains("$42.00").contains("-$15.50")
            .contains("Mar 4, 2024 10:30 AM").doesNotContain("{{");
    }

    @Test
    void everyEventTypeHasATemplateOnTheClasspath() {
        for (EventType type : EventType.values()) {
            assertThat(registry.codeFor(type)).isNotNull();
            assertThat(getClass().getResource("/templates/" + registry.codeFor(type) + ".txt"))
                .as("template for %s", type).isNotNull();
        }
    }
}
