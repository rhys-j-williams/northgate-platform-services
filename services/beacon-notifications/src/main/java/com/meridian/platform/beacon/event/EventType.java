package com.meridian.platform.beacon.event;

/** Event types on ACCT.EVENTS. The alert code column is what alerts-preferences-service keys on. */
public enum EventType {
    LARGE_DEBIT("LARGE_TXN", false),
    LARGE_CREDIT("LARGE_TXN", false),
    LOW_BALANCE("LOW_BAL", false),
    OVERDRAFT("REG_OVERDRAFT", true),
    CARD_DECLINED("CARD_DECLINE", false),
    LOGIN_NEW_DEVICE("SECURITY_LOGIN", false),
    PAYMENT_DUE("PAYMENT_DUE", false),
    RATE_CHANGE("REG_RATE_CHANGE", true),
    DISPUTE_RESOLVED("REG_ERROR_RESOLUTION", true),
    PRIVACY_NOTICE("REG_PRIVACY", true);

    private final String alertCode;
    private final boolean regulatory;

    EventType(String alertCode, boolean regulatory) {
        this.alertCode = alertCode;
        this.regulatory = regulatory;
    }

    public String alertCode() {
        return alertCode;
    }

    public boolean isRegulatory() {
        return regulatory;
    }
}
