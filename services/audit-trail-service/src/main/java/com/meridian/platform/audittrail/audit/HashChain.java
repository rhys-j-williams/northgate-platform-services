package com.meridian.platform.audittrail.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 over the previous hash and the event's stable fields. Internal Audit run
 * {@code GET /audit/v1/verify} monthly and file the result; a break means somebody has been in the
 * table with a DBA account, and that has happened exactly once (INC-2021-2207, a well-meant
 * "cleanup" of test data in prod).
 */
public final class HashChain {

    public static final String GENESIS = "0000000000000000000000000000000000000000000000000000000000000000";

    private HashChain() {
    }

    public static String hash(String prevHash, AuditEvent e) {
        String material = prevHash + "\n" + e.getEventTime() + "\n" + e.getSourceService() + "\n" + e.getEventType()
            + "\n" + e.getSubjectType() + "\n" + e.getSubjectId() + "\n" + e.getActor() + "\n" + e.getOutcome()
            + "\n" + (e.getCorrelationId() == null ? "" : e.getCorrelationId())
            + "\n" + (e.getPayload() == null ? "" : e.getPayload());
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
