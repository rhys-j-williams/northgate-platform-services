package com.meridian.platform.piivault.token;

import java.util.Locale;

/**
 * Formats we tokenise. Each says which characters are "payload" (permuted) and which are kept as
 * they are (separators, and for cards and SSNs the trailing four so masked display still works).
 */
public enum PiiType {
    SSN(4),
    CARD(4),
    ACCOUNT_NUMBER(4),
    PHONE(0),
    DOB(0);

    private final int preserveTail;

    PiiType(int preserveTail) {
        this.preserveTail = preserveTail;
    }

    public int preserveTail() {
        return preserveTail;
    }

    public static PiiType parse(String s) {
        return valueOf(s.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
