package com.meridian.platform.audittrail.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.platform.audittrail.audit.AuditEvent;
import com.meridian.platform.audittrail.audit.AuditTrailService;
import java.io.IOException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Two listeners because the topics carry different shapes. Both are idempotent on
 * (topic, offset). Poison messages are logged and skipped, not dead-lettered: the DLQ topic was
 * never created in prod (PLAT-0902) and a message that cannot be parsed is itself an audit event
 * worth keeping, so we write a PARSE_FAILED row instead.
 */
@Component
public class AuditKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaListener.class);

    private final AuditTrailService service;
    private final EventMapper mapper;
    private final ObjectMapper json;

    public AuditKafkaListener(AuditTrailService service, EventMapper mapper, ObjectMapper json) {
        this.service = service;
        this.mapper = mapper;
        this.json = json;
    }

    @KafkaListener(topics = "${meridian.audit.topics.envelope:audit.events.v1}", autoStartup = "${meridian.audit.consume:true}")
    public void onEnvelope(ConsumerRecord<String, String> record) {
        AuditEvent e;
        try {
            e = mapper.fromMessage(json.readValue(record.value(), AuditEventMessage.class));
        } catch (IOException ex) {
            log.warn("unparseable audit envelope at {}:{}", record.topic(), record.offset());
            e = parseFailed(record);
        }
        stamp(e, record);
        service.append(e);
    }

    @KafkaListener(topics = "${meridian.audit.topics.preferences:alerts.preferences.changed.v1}",
        autoStartup = "${meridian.audit.consume:true}")
    public void onPreferenceChanged(ConsumerRecord<String, String> record) {
        AuditEvent e;
        try {
            e = mapper.fromPreferenceChanged(record.value());
        } catch (IOException ex) {
            e = parseFailed(record);
        }
        stamp(e, record);
        service.append(e);
    }

    private static void stamp(AuditEvent e, ConsumerRecord<String, String> record) {
        e.setSourceTopic(record.topic());
        e.setSourceOffset(record.offset());
    }

    private static AuditEvent parseFailed(ConsumerRecord<String, String> record) {
        AuditEvent e = new AuditEvent();
        e.setSourceService("audit-trail-service");
        e.setEventType("PARSE_FAILED");
        e.setSubjectType("TOPIC");
        e.setSubjectId(record.topic());
        e.setActor("system");
        e.setOutcome("ERROR");
        e.setPayload(record.value() == null ? null : record.value().substring(0, Math.min(2000, record.value().length())));
        return e;
    }
}
