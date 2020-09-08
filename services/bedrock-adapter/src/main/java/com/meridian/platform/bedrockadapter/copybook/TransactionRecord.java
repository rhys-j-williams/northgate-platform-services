package com.meridian.platform.bedrockadapter.copybook;

import java.time.LocalDate;

/** MTBTRAN, 160 bytes. Only decoded here; posting goes through TXN-POST, not raw MTBTRAN. */
public class TransactionRecord {

    private String transactionId;
    private String accountId;
    private LocalDate postedDate;
    private LocalDate settledDate;
    private long amountMinor;
    private long runningBalanceMinor;
    private String mcc;
    private String channel;
    private String status;
    private String description;

    public static TransactionRecord decode(String record) {
        if (record.length() < Copybook.MTBTRAN_LENGTH) {
            throw new IllegalArgumentException("MTBTRAN record short: " + record.length());
        }
        TransactionRecord r = new TransactionRecord();
        r.transactionId = Fixed.trimmed(record, 0, 16);
        r.accountId = Fixed.trimmed(record, 16, 16);
        r.postedDate = Fixed.date(Fixed.slice(record, 32, 8));
        r.settledDate = Fixed.date(Fixed.slice(record, 40, 8));
        r.amountMinor = ZonedDecimal.decode(Fixed.slice(record, 48, 13));
        r.runningBalanceMinor = ZonedDecimal.decode(Fixed.slice(record, 61, 13));
        r.mcc = Fixed.trimmed(record, 74, 4);
        r.channel = Fixed.trimmed(record, 78, 8);
        r.status = Fixed.trimmed(record, 86, 10);
        r.description = Fixed.trimmed(record, 96, 64);
        return r;
    }

    public String encode() {
        return Fixed.text(transactionId, 16)
            + Fixed.text(accountId, 16)
            + Fixed.date(postedDate)
            + Fixed.date(settledDate)
            + ZonedDecimal.encode(amountMinor, 13)
            + ZonedDecimal.encode(runningBalanceMinor, 13)
            + Fixed.text(mcc, 4)
            + Fixed.text(channel, 8)
            + Fixed.text(status, 10)
            + Fixed.text(description, 64);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public LocalDate getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(LocalDate postedDate) {
        this.postedDate = postedDate;
    }

    public LocalDate getSettledDate() {
        return settledDate;
    }

    public void setSettledDate(LocalDate settledDate) {
        this.settledDate = settledDate;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public long getRunningBalanceMinor() {
        return runningBalanceMinor;
    }

    public void setRunningBalanceMinor(long runningBalanceMinor) {
        this.runningBalanceMinor = runningBalanceMinor;
    }

    public String getMcc() {
        return mcc;
    }

    public void setMcc(String mcc) {
        this.mcc = mcc;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
