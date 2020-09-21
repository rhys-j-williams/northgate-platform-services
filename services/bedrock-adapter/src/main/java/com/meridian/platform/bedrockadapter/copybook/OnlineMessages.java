package com.meridian.platform.bedrockadapter.copybook;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Encoders for the three CICS request layouts and decoders for their responses
 * (ACCT-INQ.cpy, TXN-POST.cpy, CUST-PROF.cpy). Header is shared: tran code, correlation id,
 * channel. The MQ correlation id is set separately by the gateway; the one in the record is the
 * platform correlation id so Bedrock operators can join their CICS trace to Splunk.
 */
public final class OnlineMessages {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OnlineMessages() {
    }

    public static String header(String tranCode, String correlationId, String channel) {
        return Fixed.text(tranCode, 4) + Fixed.text(correlationId, 32) + Fixed.text(channel, 4);
    }

    public static String accountInquiry(String correlationId, String channel, String accountId, LocalDate asOf) {
        String req = header(Copybook.TRAN_ACCT_INQ, correlationId, channel)
            + Fixed.text(accountId, 16)
            + (asOf == null ? "00000000" : Fixed.date(asOf));
        assertLength(req, Copybook.ACCT_INQ_REQUEST_LENGTH, "ACCT-INQ-REQUEST");
        return req;
    }

    public static String transactionPost(String correlationId, String channel, String idempotencyKey, char type,
                                         String accountId, long amountMinor, String origTranId, String description) {
        String req = header(Copybook.TRAN_TXN_POST, correlationId, channel)
            + Fixed.text(idempotencyKey, 36)
            + type
            + Fixed.text(accountId, 16)
            + ZonedDecimal.encode(amountMinor, 13)
            + Fixed.text(origTranId, 16)
            + Fixed.text(description, 32)
            + Fixed.text("", 6);
        assertLength(req, Copybook.TXN_POST_REQUEST_LENGTH, "TXN-POST-REQUEST");
        return req;
    }

    public static String customerProfile(String correlationId, String channel, String customerId, String scope) {
        String req = header(Copybook.TRAN_CUST_PROF, correlationId, channel)
            + Fixed.text(customerId, 12)
            + Fixed.text(scope, 4);
        assertLength(req, Copybook.CUST_PROF_REQUEST_LENGTH, "CUST-PROF-REQUEST");
        return req;
    }

    /** Response header common to all three: tran code, correlation, return code, abend code = 42 bytes. */
    public static ResponseHeader responseHeader(String record) {
        ResponseHeader h = new ResponseHeader();
        h.tranCode = Fixed.trimmed(record, 0, 4);
        h.correlationId = Fixed.trimmed(record, 4, 32);
        h.returnCode = Fixed.slice(record, 36, 2);
        h.abendCode = Fixed.trimmed(record, 38, 4);
        return h;
    }

    public static AccountRecord accountInquiryResponse(String record) {
        assertLength(record, Copybook.ACCT_INQ_RESPONSE_LENGTH, "ACCT-INQ-RESPONSE");
        return AccountRecord.decode(Fixed.slice(record, 64, Copybook.MTBACCT_LENGTH));
    }

    public static CustomerRecord customerProfileResponse(String record) {
        assertLength(record, Copybook.CUST_PROF_RESPONSE_LENGTH, "CUST-PROF-RESPONSE");
        return CustomerRecord.decode(Fixed.slice(record, 64, Copybook.MTBCUST_LENGTH));
    }

    public static PostingResult transactionPostResponse(String record) {
        assertLength(record, Copybook.TXN_POST_RESPONSE_LENGTH, "TXN-POST-RESPONSE");
        PostingResult r = new PostingResult();
        r.header = responseHeader(record);
        r.transactionId = Fixed.trimmed(record, 42, 16);
        String bal = Fixed.slice(record, 58, 13);
        r.newBalanceMinor = bal.trim().isEmpty() ? null : ZonedDecimal.decode(bal);
        r.reason = Fixed.trimmed(record, 71, 20);
        return r;
    }

    /** Used by the fixture fallback and by tests to build a well formed response. */
    public static String buildResponse(String tranCode, String correlationId, String rc, String abend, String body,
                                       int totalLength) {
        String ts = LocalDateTime.now(ZoneOffset.UTC).format(TS);
        String head = Fixed.text(tranCode, 4) + Fixed.text(correlationId, 32) + rc + Fixed.text(abend, 4);
        if (Copybook.TRAN_TXN_POST.equals(tranCode)) {
            return Fixed.text(head + body, totalLength);
        }
        return Fixed.text(head + ts + Fixed.text("", 8) + body, totalLength);
    }

    private static void assertLength(String record, int expected, String name) {
        if (record.length() != expected) {
            throw new IllegalStateException(name + " is " + record.length() + " bytes, copybook says " + expected);
        }
    }

    public static class ResponseHeader {
        private String tranCode;
        private String correlationId;
        private String returnCode;
        private String abendCode;

        public String getTranCode() {
            return tranCode;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public String getReturnCode() {
            return returnCode;
        }

        public String getAbendCode() {
            return abendCode;
        }

        public boolean ok() {
            return Copybook.RC_OK.equals(returnCode);
        }
    }

    public static class PostingResult {
        private ResponseHeader header;
        private String transactionId;
        private Long newBalanceMinor;
        private String reason;

        public ResponseHeader getHeader() {
            return header;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public Long getNewBalanceMinor() {
            return newBalanceMinor;
        }

        public String getReason() {
            return reason;
        }
    }
}
