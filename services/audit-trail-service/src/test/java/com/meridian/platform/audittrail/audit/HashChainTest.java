package com.meridian.platform.audittrail.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class HashChainTest {

    private static AuditEvent event(String subject) {
        AuditEvent e = new AuditEvent();
        e.setEventTime(Instant.parse("2024-03-01T10:00:00Z"));
        e.setSourceService("txn-posting-service");
        e.setEventType("POSTING");
        e.setSubjectType("ACCOUNT");
        e.setSubjectId(subject);
        e.setActor("system");
        e.setOutcome("OK");
        return e;
    }

    @Test
    void sameInputSameHash() {
        assertThat(HashChain.hash(HashChain.GENESIS, event("ACC-1"))).isEqualTo(HashChain.hash(HashChain.GENESIS, event("ACC-1")));
    }

    @Test
    void chainsOnPrevious() {
        String h1 = HashChain.hash(HashChain.GENESIS, event("ACC-1"));
        assertThat(HashChain.hash(h1, event("ACC-2"))).isNotEqualTo(HashChain.hash(HashChain.GENESIS, event("ACC-2")));
    }
}
