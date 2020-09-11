package com.meridian.platform.txnposting;

import static org.assertj.core.api.Assertions.assertThat;

import com.meridian.platform.txnposting.posting.PostingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApplicationContextTest {

    @Autowired
    private PostingService service;

    @Test
    void contextLoadsWithFlywayOnH2() {
        assertThat(service.pending()).isEmpty();
    }
}
