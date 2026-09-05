package com.meridian.platform.audittrail.audit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditTrailService {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailService.class);

    private final AuditEventRepository repository;

    public AuditTrailService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Serialisable so two appenders cannot both read the same tail and fork the chain. On DB2 this
     * is a table lock for the duration; the volume (a few hundred a second at peak) has been fine.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized AuditEvent append(AuditEvent event) {
        // Millisecond precision: DB2 TIMESTAMP keeps microseconds, the JVM gives nanos, and the
        // hash has to match what comes back out of the table (INC-2023-0115).
        event.setEventTime((event.getEventTime() == null ? Instant.now() : event.getEventTime())
            .truncatedTo(ChronoUnit.MILLIS));
        if (event.getSourceTopic() != null && event.getSourceOffset() != null
            && repository.existsBySourceTopicAndSourceOffset(event.getSourceTopic(), event.getSourceOffset())) {
            log.debug("skipping replayed offset {}:{}", event.getSourceTopic(), event.getSourceOffset());
            return null;
        }
        String prev = repository.findTopByOrderByIdDesc().map(AuditEvent::getEventHash).orElse(HashChain.GENESIS);
        event.setPrevHash(prev);
        event.setEventHash(HashChain.hash(prev, event));
        return repository.save(event);
    }

    public List<AuditEvent> forSubject(String subjectType, String subjectId) {
        return repository.findTop500BySubjectTypeAndSubjectIdOrderByEventTimeDesc(subjectType, subjectId);
    }

    public List<AuditEvent> forActor(String actor) {
        return repository.findTop500ByActorOrderByEventTimeDesc(actor);
    }

    public List<AuditEvent> forCorrelation(String correlationId) {
        return repository.findByCorrelationIdOrderByEventTimeAsc(correlationId);
    }

    /** Walks the whole chain. Fine locally; in the bank this is paged and takes about 40 minutes. */
    public Map<String, Object> verify() {
        List<AuditEvent> all = repository.findAllByOrderByIdAsc();
        String prev = HashChain.GENESIS;
        Long firstBreak = null;
        for (AuditEvent e : all) {
            if (!prev.equals(e.getPrevHash()) || !HashChain.hash(prev, e).equals(e.getEventHash())) {
                firstBreak = e.getId();
                break;
            }
            prev = e.getEventHash();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("events", all.size());
        out.put("intact", firstBreak == null);
        out.put("firstBreakAt", firstBreak);
        out.put("head", prev);
        return out;
    }
}
