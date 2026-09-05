package com.meridian.platform.entitlements;

import static org.assertj.core.api.Assertions.assertThat;

import com.meridian.platform.entitlements.entitlement.EntitlementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Replaces the Testcontainers Oracle test from 3.x; H2 in Oracle mode plus the fixture slice. */
@SpringBootTest
class ApplicationContextTest {

    @Autowired
    EntitlementRepository repository;

    @Test
    void seedsFixtureSliceIntoOracleModeH2() {
        assertThat(repository.count()).isEqualTo(5);
    }
}
