package com.meridian.platform.piivault.token;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Digit-string format preserving encryption. Ten-round Feistel over the decimal payload, HMAC-SHA256
 * round function, tweak = pii type + preserved tail. This is an FF1-shaped construction rather than
 * FF1 itself: when we built it (2020) there was no approved FF1 in the JCE provider list and the
 * commercial library was refused on cost. GIS accepted it for tokenisation-at-rest with the
 * condition that it is never described as NIST compliant. Do not call it FF1 in documentation.
 *
 * Digits that are not payload (anything non-numeric, and the preserved tail) pass through unchanged,
 * so a token has the same shape as the input: 123-45-6789 -> 806-21-6789.
 */
public final class FormatPreservingCipher {

    private static final int ROUNDS = 10;

    private final byte[] key;

    public FormatPreservingCipher(byte[] key) {
        this.key = key.clone();
    }

    public String encrypt(String plain, PiiType type) {
        return process(plain, type, true);
    }

    public String decrypt(String token, PiiType type) {
        return process(token, type, false);
    }

    private String process(String input, PiiType type, boolean forward) {
        StringBuilder digits = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        int tail = Math.min(type.preserveTail(), Math.max(0, digits.length() - 4));
        String payload = digits.substring(0, digits.length() - tail);
        String preserved = digits.substring(digits.length() - tail);
        if (payload.length() < 4) {
            throw new IllegalArgumentException("value too short to tokenise as " + type);
        }
        String tweak = type.name() + ":" + preserved;
        String out = feistel(payload, tweak, forward);

        StringBuilder result = new StringBuilder(input.length());
        int p = 0;
        int q = 0;
        for (char c : input.toCharArray()) {
            if (!Character.isDigit(c)) {
                result.append(c);
            } else if (p < out.length()) {
                result.append(out.charAt(p++));
            } else {
                result.append(preserved.charAt(q++));
            }
        }
        return result.toString();
    }

    private String feistel(String payload, String tweak, boolean forward) {
        int n = payload.length();
        int u = n / 2;
        long a = Long.parseLong(payload.substring(0, u));
        long b = Long.parseLong(payload.substring(u));
        long modA = pow10(u);
        long modB = pow10(n - u);
        if (forward) {
            for (int r = 0; r < ROUNDS; r++) {
                long f = round(r, b, tweak) % modA;
                long na = (a + f) % modA;
                a = b;
                b = na;
                long t = modA;
                modA = modB;
                modB = t;
            }
        } else {
            for (int r = ROUNDS - 1; r >= 0; r--) {
                long t = modA;
                modA = modB;
                modB = t;
                long nb = a;
                long f = round(r, nb, tweak) % modA;
                a = ((b - f) % modA + modA) % modA;
                b = nb;
            }
        }
        // after an even number of rounds the halves are back in their original slots
        return pad(a, u) + pad(b, n - u);
    }

    private long round(int r, long x, String tweak) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] d = mac.doFinal((r + "|" + x + "|" + tweak).getBytes(StandardCharsets.UTF_8));
            long v = 0;
            for (int i = 0; i < 8; i++) {
                v = (v << 8) | (d[i] & 0xff);
            }
            return v & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    private static long pow10(int n) {
        long v = 1;
        for (int i = 0; i < n; i++) {
            v *= 10;
        }
        return v;
    }

    private static String pad(long v, int width) {
        String s = Long.toString(v);
        StringBuilder sb = new StringBuilder(width);
        for (int i = s.length(); i < width; i++) {
            sb.append('0');
        }
        return sb.append(s).toString();
    }
}
