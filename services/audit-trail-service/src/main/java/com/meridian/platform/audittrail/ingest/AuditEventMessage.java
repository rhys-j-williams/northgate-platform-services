package com.meridian.platform.audittrail.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Wire shape shared by the HTTP endpoint and the Kafka topics (audit.events.v1). */
public class AuditEventMessage {

    private Instant eventTime;
    @NotBlank @Size(max = 64)
    private String sourceService;
    @NotBlank @Size(max = 64)
    private String eventType;
    @NotBlank @Size(max = 32)
    private String subjectType;
    @NotBlank @Size(max = 64)
    private String subjectId;
    @Size(max = 64)
    private String actor;
    @Size(max = 16)
    private String outcome;
    @Size(max = 64)
    private String correlationId;
    private JsonNode payload;

    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public JsonNode getPayload() { return payload; }
    public void setPayload(JsonNode payload) { this.payload = payload; }
}
