package com.meridian.platform.audittrail.audit;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "AUDIT_EVENT")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auditEventSeq")
    @SequenceGenerator(name = "auditEventSeq", sequenceName = "AUDIT_EVENT_SEQ", allocationSize = 100)
    @Column(name = "EVENT_ID")
    private Long id;

    @Column(name = "EVENT_TIME", nullable = false)
    private Instant eventTime;

    @Column(name = "RECEIVED_AT", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "SOURCE_SERVICE", nullable = false, length = 64)
    private String sourceService;

    @Column(name = "EVENT_TYPE", nullable = false, length = 64)
    private String eventType;

    @Column(name = "SUBJECT_TYPE", nullable = false, length = 32)
    private String subjectType;

    @Column(name = "SUBJECT_ID", nullable = false, length = 64)
    private String subjectId;

    @Column(name = "ACTOR", nullable = false, length = 64)
    private String actor;

    @Column(name = "OUTCOME", nullable = false, length = 16)
    private String outcome;

    @Column(name = "CORRELATION_ID", length = 64)
    private String correlationId;

    @Column(name = "SOURCE_TOPIC", length = 80)
    private String sourceTopic;

    @Column(name = "SOURCE_OFFSET")
    private Long sourceOffset;

    @Lob
    @Column(name = "PAYLOAD")
    private String payload;

    @Column(name = "PREV_HASH", nullable = false, length = 64)
    private String prevHash;

    @Column(name = "EVENT_HASH", nullable = false, length = 64)
    private String eventHash;

    public Long getId() { return id; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public Instant getReceivedAt() { return receivedAt; }
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
    public String getSourceTopic() { return sourceTopic; }
    public void setSourceTopic(String sourceTopic) { this.sourceTopic = sourceTopic; }
    public Long getSourceOffset() { return sourceOffset; }
    public void setSourceOffset(Long sourceOffset) { this.sourceOffset = sourceOffset; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }
    public String getEventHash() { return eventHash; }
    public void setEventHash(String eventHash) { this.eventHash = eventHash; }
}
