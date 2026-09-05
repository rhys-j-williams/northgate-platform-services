package com.meridian.platform.bedrockadapter.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public class PostingRequest {

    @NotBlank
    @Size(max = 36)
    private String idempotencyKey;

    @NotBlank
    @Pattern(regexp = "DEBIT|CREDIT|REVERSAL")
    private String type;

    @NotBlank
    @Size(max = 16)
    private String accountId;

    /** Minor units, always positive. Direction comes from type. */
    @Positive
    private long amountMinor;

    @Size(max = 16)
    private String originalTransactionId;

    @Size(max = 32)
    private String description;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
