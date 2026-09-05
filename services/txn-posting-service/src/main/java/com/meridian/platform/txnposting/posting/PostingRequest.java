package com.meridian.platform.txnposting.posting;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public class PostingRequest {

    @NotBlank
    @Pattern(regexp = "ACC-[0-9]{6,12}", message = "accountId must be a Meridian account id")
    private String accountId;

    @NotBlank
    @Pattern(regexp = "DEBIT|CREDIT")
    private String type;

    @Positive
    @Max(value = 99_999_999_999L, message = "amount exceeds Bedrock field width")
    private long amountMinor;

    @Size(max = 80)
    private String description;

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
