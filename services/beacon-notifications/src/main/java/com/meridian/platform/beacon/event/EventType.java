package com.meridian.platform.beacon.event;

/**
 * Event types on ACCT.EVENTS. The alert code column is what alerts-preferences-service keys on and
 * matches the codes in domain-fixtures. PAYMENT_DUE has no preference code yet (PLAT-1388); it is
 * suppressed for everyone until product decide whether it is opt-in.
 */
public enum EventType {
    LARGE_DEBIT("LARGE_TRANSACTION", false),
    LARGE_CREDIT("LARGE_TRANSACTION", false),
    LOW_BALANCE("BALANCE_LOW", false),
    OVERDRAFT("OVERDRAFT_NOTICE", true),
    CARD_DECLINED("CARD_DECLINED", false),
    LOGIN_NEW_DEVICE("SECURITY_SIGN_IN", false),
    PAYMENT_DUE("PAYMENT_DUE", false),
    RATE_CHANGE("REG_RATE_CHANGE", true),
    DISPUTE_RESOLVED("REG_E_ERROR_RESOLUTION", true),
    PRIVACY_NOTICE("PRIVACY_NOTICE", true);

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
