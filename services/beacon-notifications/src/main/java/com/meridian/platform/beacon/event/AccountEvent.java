package com.meridian.platform.beacon.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * Message on ACCT.EVENTS. Produced by txn-posting-service and by the Bedrock batch bridge
 * (nightly, via the MQ-to-CICS gateway). The sequence is per customer and is assigned by the
 * producer; Beacon only enforces it. See ADR 0002.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountEvent {

    @NotBlank
    private String eventId;
    @NotBlank
    private String customerId;
    private String accountId;
    @NotNull
    private EventType eventType;
    @Positive
    private long sequence;
    private long amountMinor;
    private long balanceAfterMinor;
    private String description;
    private String channel;
    @NotNull
    private Instant occurredAt;

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

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public long getBalanceAfterMinor() {
        return balanceAfterMinor;
    }

    public void setBalanceAfterMinor(long balanceAfterMinor) {
        this.balanceAfterMinor = balanceAfterMinor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
