package com.meridian.platform.bedrockadapter.gateway;

import com.meridian.platform.bedrockadapter.copybook.Copybook;
import com.meridian.platform.bedrockadapter.copybook.Fixed;
import com.meridian.platform.bedrockadapter.copybook.OnlineMessages;
import com.meridian.platform.bedrockadapter.copybook.ZonedDecimal;
import com.meridian.platform.common.correlation.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * The MQ-to-HTTP bridge. Puts MTBREQ envelopes on the bedrock-core-mock REST queue facade
 * (POST /mq/BEDROCK.REQ, GET /mq/BEDROCK.RESP?correlationId=) instead of a JMS destination.
 *
 * <p>MTBREQ/MTBRESP are the CICS bridge envelopes Core Banking never published; the layout lives in
 * mock-external/bedrock-core-mock/src/messages.ts, reverse engineered from a CEDF trace (PLAT-1187).
 * Our own ACCT-INQ / TXN-POST / CUST-PROF copybooks are what the *adapter* speaks internally, so
 * this class does the envelope translation both ways and hands the shared MTBACCT / MTBTRAN payload
 * records straight through untouched.
 *
 * <p>Only used where the IBM MQ image is not available. In the bank the bridge is the queue manager.
 */
public class HttpBedrockGateway implements BedrockGateway {

    private static final Logger LOG = LoggerFactory.getLogger(HttpBedrockGateway.class);

    static final int MTBREQ_LENGTH = 200;
    static final int MTBRESP_HEADER_LENGTH = 56;

    private final HttpClient http;
    private final String baseUrl;
    private final String requestQueue;
    private final String replyQueue;
    private final long waitMs;
    private final BedrockGateway customerProfileSource;

    public HttpBedrockGateway(String baseUrl, String requestQueue, String replyQueue, long waitMs,
                              BedrockGateway customerProfileSource) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.requestQueue = requestQueue;
        this.replyQueue = replyQueue;
        this.waitMs = waitMs;
        this.customerProfileSource = customerProfileSource;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @Override
    public String call(String tranCode, String request) {
        String correlationId = Fixed.trimmed(request, 4, 32);
        switch (tranCode) {
            case Copybook.TRAN_ACCT_INQ:
                return accountInquiry(correlationId, request);
            case Copybook.TRAN_TXN_POST:
                return transactionPost(correlationId, request);
            case Copybook.TRAN_CUST_PROF:
                // CUST-PROF is answered by the CIF, not the ledger; the ledger mock does not know it.
                return customerProfileSource.call(tranCode, request);
            default:
                return OnlineMessages.buildResponse(tranCode, correlationId, Copybook.RC_ABEND, "AEY9", "",
                    Copybook.ACCT_INQ_RESPONSE_LENGTH);
        }
    }

    private String accountInquiry(String correlationId, String request) {
        String accountId = Fixed.trimmed(request, Copybook.HDR_LENGTH, 16);
        String reply = exchange(mtbreq("ACCTINQ", correlationId, accountId, "", null, "", "", "RTL", ""), correlationId);
        Mtbresp r = Mtbresp.parse(reply);
        if (r.returnCode == 0 && r.count >= 1) {
            return OnlineMessages.buildResponse(Copybook.TRAN_ACCT_INQ, correlationId, Copybook.RC_OK, "",
                r.record(0, Copybook.MTBACCT_LENGTH), Copybook.ACCT_INQ_RESPONSE_LENGTH);
        }
        return OnlineMessages.buildResponse(Copybook.TRAN_ACCT_INQ, correlationId, translateRc(r), r.abendCode, "",
            Copybook.ACCT_INQ_RESPONSE_LENGTH);
    }

    private String transactionPost(String correlationId, String request) {
        int o = Copybook.HDR_LENGTH;
        String idempotencyKey = Fixed.trimmed(request, o, 36);
        char type = request.charAt(o + 36);
        String accountId = Fixed.trimmed(request, o + 37, 16);
        long amount = ZonedDecimal.decode(Fixed.slice(request, o + 53, 13));
        String description = Fixed.trimmed(request, o + 82, 32);
        long signed;
        switch (type) {
            case 'D':
                signed = -Math.abs(amount);
                break;
            case 'C':
                signed = Math.abs(amount);
                break;
            case 'R':
                // Bedrock has no reversal function online; a reversal is a contra posting carrying
                // the original amount with the sign flipped. The caller gives us the original amount.
                signed = -amount;
                break;
            default:
                return OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId, Copybook.RC_ABEND, "ASRA",
                    "", Copybook.TXN_POST_RESPONSE_LENGTH);
        }
        // REQ-TXN-ID is 16 wide; the idempotency key is a UUID. Bedrock only ever saw the first 16
        // characters and nobody has had a collision yet (PLAT-2577, open since 2022).
        String txnId = idempotencyKey.length() > 16 ? idempotencyKey.substring(0, 16) : idempotencyKey;
        String reply = exchange(mtbreq("TRANPOST", correlationId, accountId, "", signed, txnId, "", "RTL",
            description), correlationId);
        Mtbresp r = Mtbresp.parse(reply);
        if (r.returnCode == 0 || r.returnCode == 4) {
            String tran = r.count >= 1 ? r.record(0, Copybook.MTBTRAN_LENGTH) : Fixed.text("", Copybook.MTBTRAN_LENGTH);
            String bedrockTranId = Fixed.trimmed(tran, 0, 16);
            // MTBRESP for TRANPOST carries the posting, not the balance. One more round trip.
            Long balance = null;
            try {
                String inq = accountInquiry(correlationId, OnlineMessages.accountInquiry(correlationId, "RTL", accountId, null));
                if (OnlineMessages.responseHeader(inq).ok()) {
                    balance = OnlineMessages.accountInquiryResponse(inq).getAvailableBalanceMinor();
                }
            } catch (BedrockUnavailableException e) {
                LOG.warn("post-posting inquiry for {} failed: {}", accountId, e.getMessage());
            }
            String body = Fixed.text(bedrockTranId, 16)
                + (balance == null ? Fixed.text("", 13) : ZonedDecimal.encode(balance, 13))
                + Fixed.text(r.returnCode == 4 ? "DUPLICATE" : "", 20);
            return OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId,
                r.returnCode == 4 ? Copybook.RC_DUPLICATE : Copybook.RC_OK, "", body, Copybook.TXN_POST_RESPONSE_LENGTH);
        }
        if (r.returnCode == 8) {
            return OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId, Copybook.RC_RESTRICTED, "",
                Fixed.text("", 16) + Fixed.text("", 13) + Fixed.text("REFUSED", 20), Copybook.TXN_POST_RESPONSE_LENGTH);
        }
        return OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId, Copybook.RC_ABEND, r.abendCode, "",
            Copybook.TXN_POST_RESPONSE_LENGTH);
    }

    private static String translateRc(Mtbresp r) {
        switch (r.returnCode) {
            case 0:
            case 4:
                return Copybook.RC_OK;
            case 8:
                return Copybook.RC_NOT_FOUND;
            default:
                return Copybook.RC_ABEND;
        }
    }

    static String mtbreq(String func, String correlationId, String accountId, String customerId, Long amountMinor,
                         String txnId, String mcc, String channel, String description) {
        String rec = Fixed.text(func, 8)
            + Fixed.text(correlationId, 36)
            + Fixed.text(accountId, 16)
            + Fixed.text(customerId, 12)
            + (amountMinor == null ? Fixed.text("", 13) : ZonedDecimal.encode(amountMinor, 13))
            + Fixed.text(txnId, 16)
            + Fixed.text(mcc, 4)
            + Fixed.text(channel, 8)
            + Fixed.text(description, 64);
        return Fixed.text(rec, MTBREQ_LENGTH);
    }

    private String exchange(String mtbreq, String correlationId) {
        try {
            HttpRequest put = HttpRequest.newBuilder(URI.create(baseUrl + "/mq/" + requestQueue))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "text/plain")
                .header("X-Correlation-Id", correlationId)
                .header("X-Platform-Correlation-Id", CorrelationId.current())
                .POST(HttpRequest.BodyPublishers.ofString(mtbreq))
                .build();
            HttpResponse<String> putRsp = http.send(put, HttpResponse.BodyHandlers.ofString());
            if (putRsp.statusCode() / 100 != 2) {
                throw new BedrockUnavailableException("bridge refused MTBREQ: HTTP " + putRsp.statusCode());
            }
            HttpRequest get = HttpRequest.newBuilder(URI.create(baseUrl + "/mq/" + replyQueue
                    + "?correlationId=" + correlationId + "&wait=" + waitMs))
                .timeout(Duration.ofMillis(waitMs + 3000))
                .GET()
                .build();
            HttpResponse<String> getRsp = http.send(get, HttpResponse.BodyHandlers.ofString());
            if (getRsp.statusCode() == 204) {
                throw new BedrockUnavailableException("no reply from Bedrock for " + correlationId + " within " + waitMs + "ms");
            }
            if (getRsp.statusCode() != 200) {
                throw new BedrockUnavailableException("bridge reply failed: HTTP " + getRsp.statusCode());
            }
            return getRsp.body();
        } catch (IOException e) {
            throw new BedrockUnavailableException("Bedrock bridge unreachable at " + baseUrl + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BedrockUnavailableException("interrupted waiting for Bedrock", e);
        }
    }

    static final class Mtbresp {
        String func;
        String correlationId;
        int returnCode;
        String abendCode;
        int count;
        String payload;

        static Mtbresp parse(String message) {
            if (message == null || message.length() < MTBRESP_HEADER_LENGTH) {
                throw new BedrockUnavailableException("short MTBRESP header: "
                    + (message == null ? 0 : message.length()) + " bytes");
            }
            Mtbresp r = new Mtbresp();
            r.func = Fixed.trimmed(message, 0, 8);
            r.correlationId = Fixed.trimmed(message, 8, 36);
            r.returnCode = Integer.parseInt(Fixed.slice(message, 44, 4).trim());
            r.abendCode = Fixed.trimmed(message, 48, 4);
            r.count = Integer.parseInt(Fixed.slice(message, 52, 4).trim());
            r.payload = message.substring(MTBRESP_HEADER_LENGTH);
            return r;
        }

        String record(int index, int length) {
            int from = Math.min(index * length, payload.length());
            int to = Math.min(from + length, payload.length());
            return Fixed.text(payload.substring(from, to), length);
        }
    }
}
