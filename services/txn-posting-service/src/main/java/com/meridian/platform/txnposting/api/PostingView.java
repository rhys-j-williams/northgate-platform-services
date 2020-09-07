package com.meridian.platform.txnposting.api;

import com.meridian.platform.txnposting.posting.Posting;
import java.time.Instant;

public class PostingView {

    private Long postingId;
    private String idempotencyKey;
    private String accountId;
    private String type;
    private long amountMinor;
    private String description;
    private String status;
    private String bedrockTransactionId;
    private Long newBalanceMinor;
    private String refusalReason;
    private Long originalPostingId;
    private Long reversedById;
    private boolean replayed;
    private Instant createdAt;

    public static PostingView of(Posting p, boolean replayed) {
        PostingView v = new PostingView();
        v.postingId = p.getId();
        v.idempotencyKey = p.getIdempotencyKey();
        v.accountId = p.getAccountId();
        v.type = p.getType();
        v.amountMinor = p.getAmountMinor();
        v.description = p.getDescription();
        v.status = p.getStatus();
        v.bedrockTransactionId = p.getBedrockTransactionId();
        v.newBalanceMinor = p.getNewBalanceMinor();
        v.refusalReason = p.getRefusalReason();
        v.originalPostingId = p.getOriginalPostingId();
        v.reversedById = p.getReversedById();
        v.replayed = replayed;
        v.createdAt = p.getCreatedAt();
        return v;
    }

    public Long getPostingId() { return postingId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getAccountId() { return accountId; }
    public String getType() { return type; }
    public long getAmountMinor() { return amountMinor; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getBedrockTransactionId() { return bedrockTransactionId; }
    public Long getNewBalanceMinor() { return newBalanceMinor; }
    public String getRefusalReason() { return refusalReason; }
    public Long getOriginalPostingId() { return originalPostingId; }
    public Long getReversedById() { return reversedById; }
    public boolean isReplayed() { return replayed; }
    public Instant getCreatedAt() { return createdAt; }
}
