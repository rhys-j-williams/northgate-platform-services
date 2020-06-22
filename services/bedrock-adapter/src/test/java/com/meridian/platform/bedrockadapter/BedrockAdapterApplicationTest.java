package com.meridian.platform.bedrockadapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.meridian.platform.bedrockadapter.api.BedrockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BedrockAdapterApplicationTest {

    @Autowired
    private BedrockService service;

    @Test
    void contextLoadsInFixtureMode() {
        assertThat(service.status()).containsEntry("mode", "fixture");
    }
}
