package com.meridian.platform.alertprefs.preference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.platform.alertprefs.publish.PreferenceChangedEvent;
import com.meridian.platform.alertprefs.publish.PreferencePublisher;
import com.meridian.platform.common.error.ApiException;
import com.meridian.platform.common.fixtures.FixtureStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PreferenceServiceTest {

    private AlertPreferenceRepository repository;
    private AlertPreferenceHistoryRepository history;
    private PreferencePublisher publisher;
    private PreferenceService service;
    private final List<AlertPreference> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = mock(AlertPreferenceRepository.class);
        history = mock(AlertPreferenceHistoryRepository.class);
        publisher = mock(PreferencePublisher.class);
        when(repository.save(any())).thenAnswer(inv -> {
            AlertPreference p = inv.getArgument(0);
            saved.add(p);
            return p;
        });
        service = new PreferenceService(repository, history, publisher,
            new FixtureStore(new ObjectMapper().createObjectNode()));
    }

    @Test
    void seedsCatalogueWhenCustomerHasNoRows() {
        when(repository.findByCustomerIdOrderByAlertCodeAsc("CUS-1")).thenReturn(Collections.emptyList());
        List<AlertPreference> prefs = service.forCustomer("CUS-1");
        assertThat(prefs).hasSize(AlertCatalogue.entries().size());
        assertThat(prefs).allMatch(AlertPreference::isEnabled);
        assertThat(prefs).filteredOn(AlertPreference::isRegulatory).hasSize(4);
    }

    @Test
    void updateWritesHistoryAndPublishes() {
        AlertPreference existing = new AlertPreference();
        existing.setCustomerId("CUS-1");
        existing.setAlertCode("BALANCE_LOW");
        existing.setChannels(Arrays.asList("EMAIL"));
        when(repository.findByCustomerIdOrderByAlertCodeAsc("CUS-1")).thenReturn(Collections.singletonList(existing));
        when(repository.findByCustomerIdAndAlertCode("CUS-1", "BALANCE_LOW")).thenReturn(Optional.of(existing));

        PreferenceUpdate u = new PreferenceUpdate();
        u.setEnabled(false);
        u.setThresholdMinor(5000L);
        AlertPreference result = service.update("CUS-1", "BALANCE_LOW", u, "cust:CUS-1");

        assertThat(result.isEnabled()).isFalse();
        assertThat(result.getThresholdMinor()).isEqualTo(5000L);
        verify(history).save(any(AlertPreferenceHistory.class));
        ArgumentCaptor<PreferenceChangedEvent> captor = ArgumentCaptor.forClass(PreferenceChangedEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().getAlertCode()).isEqualTo("BALANCE_LOW");
        assertThat(captor.getValue().isEnabled()).isFalse();
    }

    @Test
    void regulatoryDisableDoesNotTouchStorage() {
        AlertPreference existing = new AlertPreference();
        existing.setCustomerId("CUS-1");
        existing.setAlertCode("PRIVACY_NOTICE");
        existing.setRegulatory(true);
        existing.setChannels(Arrays.asList("EMAIL"));
        when(repository.findByCustomerIdOrderByAlertCodeAsc("CUS-1")).thenReturn(Collections.singletonList(existing));
        when(repository.findByCustomerIdAndAlertCode("CUS-1", "PRIVACY_NOTICE")).thenReturn(Optional.of(existing));

        PreferenceUpdate u = new PreferenceUpdate();
        u.setEnabled(false);
        assertThatThrownBy(() -> service.update("CUS-1", "PRIVACY_NOTICE", u, "cust:CUS-1")).isInstanceOf(ApiException.class);
        verify(history, never()).save(any());
        verify(publisher, never()).publish(any());
        assertThat(saved).isEmpty();
    }

    @Test
    void unknownCodeIs404() {
        when(repository.findByCustomerIdOrderByAlertCodeAsc("CUS-1")).thenReturn(Collections.singletonList(new AlertPreference()));
        when(repository.findByCustomerIdAndAlertCode("CUS-1", "NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get("CUS-1", "NOPE"))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("PREFERENCE_NOT_FOUND"));
    }
}
