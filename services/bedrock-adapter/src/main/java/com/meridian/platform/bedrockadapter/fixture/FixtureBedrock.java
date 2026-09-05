package com.meridian.platform.bedrockadapter.fixture;

import com.fasterxml.jackson.databind.JsonNode;
import com.meridian.platform.bedrockadapter.copybook.AccountRecord;
import com.meridian.platform.bedrockadapter.copybook.Copybook;
import com.meridian.platform.bedrockadapter.copybook.CustomerRecord;
import com.meridian.platform.bedrockadapter.copybook.Fixed;
import com.meridian.platform.bedrockadapter.copybook.OnlineMessages;
import com.meridian.platform.bedrockadapter.copybook.ZonedDecimal;
import com.meridian.platform.bedrockadapter.gateway.BedrockGateway;
import com.meridian.platform.common.fixtures.FixtureStore;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * An in-process Bedrock that speaks the same fixed width records as the real one, answering out
 * of the shared fixture set. It is what the adapter talks to when MQ is not there. Postings are
 * remembered in memory so a debit followed by an inquiry shows the new balance, and idempotency
 * keys are honoured the way Bedrock honours them (RC 20, original result echoed).
 *
 * It is not the mock in mock-external/bedrock-core-mock. That one sits behind MQ and is what the
 * integration environment uses; this one is the fallback for a laptop with nothing else running.
 */
@Component
public class FixtureBedrock implements BedrockGateway {

    private final FixtureStore fixtures;
    private final Map<String, Long> balanceOverrides = new ConcurrentHashMap<>();
    private final Map<String, String> postingsByIdempotencyKey = new ConcurrentHashMap<>();
    private final Map<String, String> postingsByTranId = new ConcurrentHashMap<>();
    private final AtomicLong tranSeq = new AtomicLong(900_000_000L);

    public FixtureBedrock(FixtureStore fixtures) {
        this.fixtures = fixtures;
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
                return customerProfile(correlationId, request);
            default:
                return OnlineMessages.buildResponse(tranCode, correlationId, Copybook.RC_ABEND, "AEY7", "",
                    Copybook.ACCT_INQ_RESPONSE_LENGTH);
        }
    }

    private String accountInquiry(String correlationId, String request) {
        String accountId = Fixed.trimmed(request, Copybook.HDR_LENGTH, 16);
        Optional<AccountRecord> record = findAccount(accountId);
        if (record.isEmpty()) {
            return OnlineMessages.buildResponse(Copybook.TRAN_ACCT_INQ, correlationId, Copybook.RC_NOT_FOUND, "",
                "", Copybook.ACCT_INQ_RESPONSE_LENGTH);
        }
        AccountRecord acct = record.get();
        if ("RESTRICTED".equals(acct.getStatus())) {
            return OnlineMessages.buildResponse(Copybook.TRAN_ACCT_INQ, correlationId, Copybook.RC_RESTRICTED, "",
                "", Copybook.ACCT_INQ_RESPONSE_LENGTH);
        }
        return OnlineMessages.buildResponse(Copybook.TRAN_ACCT_INQ, correlationId, Copybook.RC_OK, "",
            acct.encode(), Copybook.ACCT_INQ_RESPONSE_LENGTH);
    }

    private String transactionPost(String correlationId, String request) {
        int o = Copybook.HDR_LENGTH;
        String idempotencyKey = Fixed.trimmed(request, o, 36);
        char type = request.charAt(o + 36);
        String accountId = Fixed.trimmed(request, o + 37, 16);
        long amount = ZonedDecimal.decode(Fixed.slice(request, o + 53, 13));
        String origTranId = Fixed.trimmed(request, o + 66, 16);

        if (!idempotencyKey.isEmpty() && postingsByIdempotencyKey.containsKey(idempotencyKey)) {
            String original = postingsByIdempotencyKey.get(idempotencyKey);
            // Bedrock echoes the original body under RC 20 so the caller can reconcile.
            return OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId, Copybook.RC_DUPLICATE, "",
                original.substring(42), Copybook.TXN_POST_RESPONSE_LENGTH);
        }
        Optional<AccountRecord> record = findAccount(accountId);
        if (record.isEmpty()) {
            return OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId, Copybook.RC_NOT_FOUND, "",
                "", Copybook.TXN_POST_RESPONSE_LENGTH);
        }
        AccountRecord acct = record.get();
        if ("RESTRICTED".equals(acct.getStatus()) || "CLOSED".equals(acct.getStatus())) {
            return OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId, Copybook.RC_RESTRICTED, "",
                Fixed.text("", 16) + Fixed.text("", 13) + Fixed.text("RESTRICTED", 20),
                Copybook.TXN_POST_RESPONSE_LENGTH);
        }
        long signed;
        switch (type) {
            case 'D':
                signed = -Math.abs(amount);
                break;
            case 'C':
                signed = Math.abs(amount);
                break;
            case 'R':
                if (origTranId.isEmpty() || !postingsByTranId.containsKey(origTranId)) {
                    return OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId,
                        Copybook.RC_REVERSAL_REFUSED, "",
                        Fixed.text("", 16) + Fixed.text("", 13) + Fixed.text("ORIG-NOT-FOUND", 20),
                        Copybook.TXN_POST_RESPONSE_LENGTH);
                }
                // Reversal amount is the original amount negated; a partial reversal is refused.
                long origAmount = Long.parseLong(postingsByTranId.get(origTranId));
                if (Math.abs(amount) != Math.abs(origAmount)) {
                    return OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId,
                        Copybook.RC_REVERSAL_REFUSED, "",
                        Fixed.text("", 16) + Fixed.text("", 13) + Fixed.text("PARTIAL-REVERSAL", 20),
                        Copybook.TXN_POST_RESPONSE_LENGTH);
                }
                signed = -origAmount;
                break;
            default:
                return OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId, Copybook.RC_ABEND, "ASRA",
                    "", Copybook.TXN_POST_RESPONSE_LENGTH);
        }
        long current = balanceOverrides.getOrDefault(accountId, acct.getAvailableBalanceMinor());
        boolean depositAccount = !acct.getType().contains("CREDIT") && !acct.getType().contains("LOAN")
            && !acct.getType().contains("MORTGAGE");
        if (depositAccount && current + signed < 0) {
            return OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId, Copybook.RC_RESTRICTED, "",
                Fixed.text("", 16) + ZonedDecimal.encode(current, 13) + Fixed.text("NSF", 20),
                Copybook.TXN_POST_RESPONSE_LENGTH);
        }
        long updated = current + signed;
        balanceOverrides.put(accountId, updated);
        String tranId = "TXN-" + tranSeq.incrementAndGet();
        postingsByTranId.put(tranId, Long.toString(signed));
        String body = Fixed.text(tranId, 16) + ZonedDecimal.encode(updated, 13) + Fixed.text("", 20);
        String response = OnlineMessages.buildResponse(Copybook.TRAN_TXN_POST, correlationId, Copybook.RC_OK, "",
            body, Copybook.TXN_POST_RESPONSE_LENGTH);
        if (!idempotencyKey.isEmpty()) {
            postingsByIdempotencyKey.put(idempotencyKey, response);
        }
        return response;
    }

    private String customerProfile(String correlationId, String request) {
        String customerId = Fixed.trimmed(request, Copybook.HDR_LENGTH, 12);
        String scope = Fixed.trimmed(request, Copybook.HDR_LENGTH + 12, 4);
        Optional<JsonNode> customer = fixtures.customer(customerId);
        if (customer.isEmpty()) {
            return OnlineMessages.buildResponse(Copybook.TRAN_CUST_PROF, correlationId, Copybook.RC_NOT_FOUND, "",
                "", Copybook.CUST_PROF_RESPONSE_LENGTH);
        }
        CustomerRecord rec = toCustomerRecord(customer.get());
        if ("NAME".equals(scope)) {
            rec = rec.nameScopeOnly();
        }
        return OnlineMessages.buildResponse(Copybook.TRAN_CUST_PROF, correlationId, Copybook.RC_OK, "",
            rec.encode(), Copybook.CUST_PROF_RESPONSE_LENGTH);
    }

    private Optional<AccountRecord> findAccount(String accountId) {
        for (String raw : fixtures.bedrockAccountRecords()) {
            if (raw.startsWith(Fixed.text(accountId, 16))) {
                AccountRecord r = AccountRecord.decode(raw);
                Long override = balanceOverrides.get(accountId);
                if (override != null) {
                    r.setAvailableBalanceMinor(override);
                    r.setCurrentBalanceMinor(override);
                }
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    static CustomerRecord toCustomerRecord(JsonNode c) {
        CustomerRecord r = new CustomerRecord();
        r.setCustomerId(c.path("customerId").asText());
        r.setSegment(c.path("segment").asText().toUpperCase());
        r.setFirstName(c.path("firstName").asText());
        r.setLastName(c.path("lastName").asText());
        r.setOrgName(c.path("organisationName").asText(""));
        String last4 = c.path("taxIdLastFour").asText("");
        r.setTaxIdToken(last4.isEmpty() ? "" : "tok_ssn_" + last4 + "_fixture");
        r.setMobile(c.path("mobile").asText(""));
        r.setEnrolledDate(LocalDate.parse(c.path("enrolledAt").asText().substring(0, 10)));
        r.setPostal(c.path("address").path("postalCode").asText(""));
        r.setState(c.path("address").path("state").asText(""));
        r.setPaperless(true);
        return r;
    }
}
