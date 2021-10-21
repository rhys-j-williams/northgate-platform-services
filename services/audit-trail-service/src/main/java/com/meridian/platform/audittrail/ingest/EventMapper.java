package com.meridian.platform.audittrail.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.platform.audittrail.audit.AuditEvent;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    private final ObjectMapper mapper;

    public EventMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public AuditEvent fromMessage(AuditEventMessage m) {
        AuditEvent e = new AuditEvent();
        e.setEventTime(m.getEventTime() == null ? Instant.now() : m.getEventTime());
        e.setSourceService(m.getSourceService());
        e.setEventType(m.getEventType());
        e.setSubjectType(m.getSubjectType());
        e.setSubjectId(m.getSubjectId());
        e.setActor(m.getActor() == null ? "system" : m.getActor());
        e.setOutcome(m.getOutcome() == null ? "OK" : m.getOutcome());
        e.setCorrelationId(m.getCorrelationId());
        e.setPayload(m.getPayload() == null ? null : m.getPayload().toString());
        return e;
    }

    /**
     * alerts-preferences publishes its own shape (PreferenceChangedEvent) rather than the audit
     * envelope. PLAT-1055 was raised to make them conform; they closed it Won't Fix, so we adapt.
     */
    public AuditEvent fromPreferenceChanged(String json) throws java.io.IOException {
        JsonNode n = mapper.readTree(json);
        AuditEvent e = new AuditEvent();
        e.setEventTime(n.hasNonNull("changedAt") ? Instant.parse(n.get("changedAt").asText()) : Instant.now());
        e.setSourceService("alerts-preferences-service");
        e.setEventType("PREFERENCE_CHANGED:" + n.path("alertCode").asText("?"));
        e.setSubjectType("CUSTOMER");
        e.setSubjectId(n.path("customerId").asText("?"));
        e.setActor(n.path("changedBy").asText("anonymous"));
        e.setOutcome("OK");
        e.setCorrelationId(n.path("correlationId").asText(null));
        e.setPayload(json);
        return e;
    }
}
