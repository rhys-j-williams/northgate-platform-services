package com.meridian.platform.txnposting.bedrock;

public class BedrockPostingResult {

    public enum Outcome { POSTED, DUPLICATE, REFUSED, UNAVAILABLE }

    private final Outcome outcome;
    private final String transactionId;
    private final Long newBalanceMinor;
    private final String reason;

    public BedrockPostingResult(Outcome outcome, String transactionId, Long newBalanceMinor, String reason) {
        this.outcome = outcome;
        this.transactionId = transactionId;
        this.newBalanceMinor = newBalanceMinor;
        this.reason = reason;
    }

    public static BedrockPostingResult unavailable(String reason) {
        return new BedrockPostingResult(Outcome.UNAVAILABLE, null, null, reason);
    }

    public Outcome getOutcome() { return outcome; }
    public String getTransactionId() { return transactionId; }
    public Long getNewBalanceMinor() { return newBalanceMinor; }
    public String getReason() { return reason; }
}
