package com.meridian.platform.txnposting.posting;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Request fingerprint used to tell "same key, same request" (replay) from "same key, different
 * request" (client bug, 409). Description is deliberately excluded: the mobile app appends a
 * retry counter to it (MOB-4471) and we do not want that to defeat the replay.
 */
public final class IdempotencyKeys {

    private IdempotencyKeys() {
    }

    public static String fingerprint(PostingRequest request) {
        String material = request.getAccountId() + "|" + request.getType() + "|" + request.getAmountMinor();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean wellFormed(String key) {
        return key != null && key.length() >= 8 && key.length() <= 36 && key.matches("[A-Za-z0-9._-]+");
    }
}
