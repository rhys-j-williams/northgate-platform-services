package com.meridian.platform.txnposting.posting;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "POSTING")
public class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "postingSeq")
    @SequenceGenerator(name = "postingSeq", sequenceName = "POSTING_SEQ", allocationSize = 50)
    @Column(name = "POSTING_ID")
    private Long id;

    @Column(name = "IDEMPOTENCY_KEY", nullable = false, length = 36)
    private String idempotencyKey;

    @Column(name = "REQUEST_HASH", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "ACCOUNT_ID", nullable = false, length = 16)
    private String accountId;

    @Column(name = "POSTING_TYPE", nullable = false, length = 8)
    private String type;

    @Column(name = "AMOUNT_MINOR", nullable = false)
    private long amountMinor;

    @Column(name = "DESCRIPTION", length = 80)
    private String description;

    @Column(name = "CHANNEL", nullable = false, length = 3)
    private String channel;

    @Column(name = "STATUS", nullable = false, length = 16)
    private String status;

    @Column(name = "BEDROCK_TRAN_ID", length = 16)
    private String bedrockTransactionId;

    @Column(name = "NEW_BALANCE_MINOR")
    private Long newBalanceMinor;

    @Column(name = "REFUSAL_REASON", length = 40)
    private String refusalReason;

    @Column(name = "ORIGINAL_POSTING_ID")
    private Long originalPostingId;

    @Column(name = "REVERSED_BY_ID")
    private Long reversedById;

    @Column(name = "CORRELATION_ID", length = 64)
    private String correlationId;

    @Column(name = "CREATED_AT", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBedrockTransactionId() { return bedrockTransactionId; }
    public void setBedrockTransactionId(String bedrockTransactionId) { this.bedrockTransactionId = bedrockTransactionId; }
    public Long getNewBalanceMinor() { return newBalanceMinor; }
    public void setNewBalanceMinor(Long newBalanceMinor) { this.newBalanceMinor = newBalanceMinor; }
    public String getRefusalReason() { return refusalReason; }
    public void setRefusalReason(String refusalReason) { this.refusalReason = refusalReason; }
    public Long getOriginalPostingId() { return originalPostingId; }
    public void setOriginalPostingId(Long originalPostingId) { this.originalPostingId = originalPostingId; }
    public Long getReversedById() { return reversedById; }
    public void setReversedById(Long reversedById) { this.reversedById = reversedById; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
