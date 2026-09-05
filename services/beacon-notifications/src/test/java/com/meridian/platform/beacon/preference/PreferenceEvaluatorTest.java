package com.meridian.platform.beacon.preference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.meridian.platform.beacon.event.AccountEvent;
import com.meridian.platform.beacon.event.EventType;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PreferenceEvaluatorTest {

    private final PreferencesClient client = mock(PreferencesClient.class);
    private final PreferenceEvaluator evaluator = new PreferenceEvaluator(client);
    private final CustomerPreferences prefs = new CustomerPreferences();

    @BeforeEach
    void setUp() {
        prefs.setCustomerId("CUS-1");
        when(client.forCustomer(anyString())).thenReturn(prefs);
    }

    @Test
    void regulatoryEventNotifiesEvenWhenCustomerDisabledIt() {
        CustomerPreferences.AlertPreference p = new CustomerPreferences.AlertPreference();
        p.setAlertCode("REG_OVERDRAFT");
        p.setEnabled(false);
        p.setChannels(Collections.singletonList("PUSH"));
        prefs.getAlerts().put("REG_OVERDRAFT", p);

        PreferenceDecision d = evaluator.evaluate(event(EventType.OVERDRAFT, -100));

        assertThat(d.shouldNotify()).isTrue();
        // push is not durable, so the regulatory minimum kicks in
        assertThat(d.channels()).containsExactly("EMAIL");
    }

    @Test
    void thresholdSuppressesSmallDebits() {
        CustomerPreferences.AlertPreference p = new CustomerPreferences.AlertPreference();
        p.setAlertCode("LARGE_TXN");
        p.setEnabled(true);
        p.setThresholdMinor(50_000L);
        p.setChannels(Arrays.asList("PUSH", "SMS"));
        prefs.getAlerts().put("LARGE_TXN", p);

        assertThat(evaluator.evaluate(event(EventType.LARGE_DEBIT, -49_999)).shouldNotify()).isFalse();
        PreferenceDecision big = evaluator.evaluate(event(EventType.LARGE_DEBIT, -50_000));
        assertThat(big.shouldNotify()).isTrue();
        assertThat(big.channels()).containsExactly("PUSH", "SMS");
    }

    @Test
    void missingPreferenceSuppressesNonRegulatory() {
        assertThat(evaluator.evaluate(event(EventType.CARD_DECLINED, -10)).reason()).contains("no preference");
    }

    private static AccountEvent event(EventType type, long amount) {
        AccountEvent e = new AccountEvent();
        e.setEventId("E");
        e.setCustomerId("CUS-1");
        e.setAccountId("ACC-000000001");
        e.setEventType(type);
        e.setSequence(1);
        e.setAmountMinor(amount);
        e.setOccurredAt(Instant.now());
        return e;
    }
}
