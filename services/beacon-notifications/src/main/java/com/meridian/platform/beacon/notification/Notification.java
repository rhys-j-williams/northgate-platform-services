package com.meridian.platform.beacon.notification;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "BEACON_NOTIFICATION")
public class Notification {

    public enum Status { PENDING, SENT, FAILED, SUPPRESSED }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "beacon_notification_seq")
    @SequenceGenerator(name = "beacon_notification_seq", sequenceName = "BEACON_NOTIFICATION_SEQ", allocationSize = 50)
    @Column(name = "NOTIFICATION_ID")
    private Long notificationId;

    @Column(name = "EVENT_ID", nullable = false, length = 64)
    private String eventId;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 16)
    private String customerId;

    @Column(name = "ACCOUNT_ID", length = 16)
    private String accountId;

    @Column(name = "CUSTOMER_SEQUENCE", nullable = false)
    private long customerSequence;

    @Column(name = "EVENT_TYPE", nullable = false, length = 40)
    private String eventType;

    @Column(name = "TEMPLATE_CODE", nullable = false, length = 40)
    private String templateCode;

    @Column(name = "CHANNEL", nullable = false, length = 10)
    private String channel;

    @Column(name = "REGULATORY", nullable = false)
    private boolean regulatory;

    @Column(name = "RENDERED_SUBJECT", length = 200)
    private String renderedSubject;

    @Lob
    @Column(name = "RENDERED_BODY")
    private String renderedBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 12)
    private Status status = Status.PENDING;

    @Column(name = "FAILURE_REASON", length = 400)
    private String failureReason;

    @Column(name = "ATTEMPTS", nullable = false)
    private int attempts;

    @Column(name = "OCCURRED_AT", nullable = false)
    private Instant occurredAt;

    @Column(name = "DISPATCHED_AT")
    private Instant dispatchedAt;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getNotificationId() {
        return notificationId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public long getCustomerSequence() {
        return customerSequence;
    }

    public void setCustomerSequence(long customerSequence) {
        this.customerSequence = customerSequence;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isRegulatory() {
        return regulatory;
    }

    public void setRegulatory(boolean regulatory) {
        this.regulatory = regulatory;
    }

    public String getRenderedSubject() {
        return renderedSubject;
    }

    public void setRenderedSubject(String renderedSubject) {
        this.renderedSubject = renderedSubject;
    }

    public String getRenderedBody() {
        return renderedBody;
    }

    public void setRenderedBody(String renderedBody) {
        this.renderedBody = renderedBody;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
