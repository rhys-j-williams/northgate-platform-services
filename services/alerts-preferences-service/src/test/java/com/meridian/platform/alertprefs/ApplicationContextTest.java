package com.meridian.platform.alertprefs;

import static org.assertj.core.api.Assertions.assertThat;

import com.meridian.platform.alertprefs.preference.PreferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApplicationContextTest {

    @Autowired
    private PreferenceService service;

    @Test
    void contextLoadsWithFlywayOnH2() {
        // Seeds from the catalogue; fixtures may or may not be on the path in CI.
        assertThat(service.forCustomer("CUS-100000")).hasSize(10);
    }
}
