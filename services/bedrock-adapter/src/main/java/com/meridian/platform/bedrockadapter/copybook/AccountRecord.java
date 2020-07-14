package com.meridian.platform.bedrockadapter.copybook;

import java.time.LocalDate;

/** MTBACCT, 136 bytes. */
public class AccountRecord {

    private String accountId;
    private String customerId;
    private String type;
    private String accountNumber;
    private String routingNumber;
    private long currentBalanceMinor;
    private long availableBalanceMinor;
    private LocalDate openedDate;
    private String status;
    private String ownerName;

    public static AccountRecord decode(String record) {
        if (record.length() < Copybook.MTBACCT_LENGTH) {
            throw new IllegalArgumentException("MTBACCT record short: " + record.length());
        }
        AccountRecord r = new AccountRecord();
        r.accountId = Fixed.trimmed(record, 0, 16);
        r.customerId = Fixed.trimmed(record, 16, 12);
        r.type = Fixed.trimmed(record, 28, 20);
        r.accountNumber = Fixed.trimmed(record, 48, 10);
        r.routingNumber = Fixed.trimmed(record, 58, 9);
        r.currentBalanceMinor = ZonedDecimal.decode(Fixed.slice(record, 67, 13));
        r.availableBalanceMinor = ZonedDecimal.decode(Fixed.slice(record, 80, 13));
        r.openedDate = Fixed.date(Fixed.slice(record, 93, 8));
        r.status = Fixed.trimmed(record, 101, 10);
        r.ownerName = Fixed.trimmed(record, 111, 25);
        return r;
    }

    public String encode() {
        return Fixed.text(accountId, 16)
            + Fixed.text(customerId, 12)
            + Fixed.text(type, 20)
            + Fixed.text(accountNumber, 10)
            + Fixed.text(routingNumber, 9)
            + ZonedDecimal.encode(currentBalanceMinor, 13)
            + ZonedDecimal.encode(availableBalanceMinor, 13)
            + Fixed.date(openedDate)
            + Fixed.text(status, 10)
            + Fixed.text(ownerName, 25);
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getRoutingNumber() {
        return routingNumber;
    }

    public void setRoutingNumber(String routingNumber) {
        this.routingNumber = routingNumber;
    }

    public long getCurrentBalanceMinor() {
        return currentBalanceMinor;
    }

    public void setCurrentBalanceMinor(long currentBalanceMinor) {
        this.currentBalanceMinor = currentBalanceMinor;
    }

    public long getAvailableBalanceMinor() {
        return availableBalanceMinor;
    }

    public void setAvailableBalanceMinor(long availableBalanceMinor) {
        this.availableBalanceMinor = availableBalanceMinor;
    }

    public LocalDate getOpenedDate() {
        return openedDate;
    }

    public void setOpenedDate(LocalDate openedDate) {
        this.openedDate = openedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}
