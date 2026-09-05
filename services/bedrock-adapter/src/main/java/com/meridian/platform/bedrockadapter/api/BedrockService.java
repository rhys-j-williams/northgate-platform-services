package com.meridian.platform.bedrockadapter.api;

import com.meridian.platform.bedrockadapter.copybook.AccountRecord;
import com.meridian.platform.bedrockadapter.copybook.Copybook;
import com.meridian.platform.bedrockadapter.copybook.CustomerRecord;
import com.meridian.platform.bedrockadapter.copybook.Fixed;
import com.meridian.platform.bedrockadapter.copybook.OnlineMessages;
import com.meridian.platform.bedrockadapter.copybook.OnlineMessages.PostingResult;
import com.meridian.platform.bedrockadapter.copybook.OnlineMessages.ResponseHeader;
import com.meridian.platform.bedrockadapter.copybook.TransactionRecord;
import com.meridian.platform.bedrockadapter.gateway.BedrockGateway;
import com.meridian.platform.bedrockadapter.gateway.BedrockUnavailableException;
import com.meridian.platform.common.correlation.CorrelationId;
import com.meridian.platform.common.error.ApiException;
import com.meridian.platform.common.fixtures.FixtureStore;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BedrockService {

    private static final Logger LOG = LoggerFactory.getLogger(BedrockService.class);

    private final BedrockGateway gateway;
    private final FixtureStore fixtures;
    private final String mode;

    public BedrockService(BedrockGateway gateway, FixtureStore fixtures,
                          @Value("${meridian.bedrock.mode:auto}") String mode) {
        this.gateway = gateway;
        this.fixtures = fixtures;
        this.mode = mode;
    }

    public AccountRecord inquireAccount(String accountId, String channel, LocalDate asOf) {
        String req = OnlineMessages.accountInquiry(CorrelationId.current(), channel(channel), accountId, asOf);
        String rsp = call(Copybook.TRAN_ACCT_INQ, req);
        ResponseHeader h = OnlineMessages.responseHeader(rsp);
        if (!h.ok()) {
            throw translate(h, "account " + accountId);
        }
        return OnlineMessages.accountInquiryResponse(rsp);
    }

    /**
     * There is no online transaction history transaction in Bedrock; history comes off the
     * nightly MTBTRAN extract. Locally that is the fixture file. In the bank it is the DB2 ODS
     * that statements-api reads. Kept here so the BFFs have one place to ask. PLAT-2010.
     */
    public List<TransactionRecord> transactions(String accountId, int limit) {
        List<TransactionRecord> out = new ArrayList<>();
        String prefix = Fixed.text(accountId, 16);
        for (String raw : fixtures.bedrockTransactionRecords()) {
            if (raw.regionMatches(16, prefix, 0, 16)) {
                out.add(TransactionRecord.decode(raw));
                if (out.size() >= limit) {
                    break;
                }
            }
        }
        return out;
    }

    public List<AccountRecord> accountsForCustomer(String customerId) {
        List<AccountRecord> out = new ArrayList<>();
        String prefix = Fixed.text(customerId, 12);
        for (String raw : fixtures.bedrockAccountRecords()) {
            if (raw.regionMatches(16, prefix, 0, 12)) {
                try {
                    out.add(inquireAccount(Fixed.trimmed(raw, 0, 16), "RTL", null));
                } catch (ApiException e) {
                    // restricted accounts are omitted from the list, same as the CICS screen does
                    LOG.debug("skipping account for {}: {}", customerId, e.getMessage());
                }
            }
        }
        return out;
    }

    public CustomerRecord customerProfile(String customerId, String channel, String scope) {
        String req = OnlineMessages.customerProfile(CorrelationId.current(), channel(channel), customerId, scope);
        String rsp = call(Copybook.TRAN_CUST_PROF, req);
        ResponseHeader h = OnlineMessages.responseHeader(rsp);
        if (!h.ok()) {
            throw translate(h, "customer " + customerId);
        }
        return OnlineMessages.customerProfileResponse(rsp);
    }

    public PostingResult post(PostingRequest body, String channel) {
        char type = body.getType().charAt(0);
        String req = OnlineMessages.transactionPost(CorrelationId.current(), channel(channel),
            body.getIdempotencyKey(), type, body.getAccountId(), body.getAmountMinor(),
            body.getOriginalTransactionId(), body.getDescription());
        String rsp = call(Copybook.TRAN_TXN_POST, req);
        PostingResult result = OnlineMessages.transactionPostResponse(rsp);
        String rc = result.getHeader().getReturnCode();
        if (Copybook.RC_OK.equals(rc) || Copybook.RC_DUPLICATE.equals(rc)) {
            return result;
        }
        if (Copybook.RC_REVERSAL_REFUSED.equals(rc)) {
            throw ApiException.conflict("BEDROCK_REVERSAL_REFUSED", "reversal refused: " + result.getReason());
        }
        if (Copybook.RC_RESTRICTED.equals(rc)) {
            throw ApiException.conflict("BEDROCK_POSTING_REFUSED", "posting refused: " + result.getReason());
        }
        throw translate(result.getHeader(), "posting");
    }

    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", mode);
        m.put("gateway", gateway.getClass().getSimpleName());
        m.put("fixtureSeed", fixtures.seed());
        m.put("fixtureAccounts", fixtures.bedrockAccountRecords().size());
        return m;
    }

    private String call(String tranCode, String request) {
        try {
            return gateway.call(tranCode, request);
        } catch (BedrockUnavailableException e) {
            throw ApiException.upstream("BEDROCK_UNAVAILABLE", e.getMessage());
        }
    }

    private static String channel(String channel) {
        return channel == null ? "RTL" : channel.toUpperCase();
    }

    private static ApiException translate(ResponseHeader h, String what) {
        switch (h.getReturnCode()) {
            case Copybook.RC_NOT_FOUND:
                return ApiException.notFound("BEDROCK_NOT_FOUND", what + " not found");
            case Copybook.RC_RESTRICTED:
                return ApiException.forbidden("BEDROCK_RESTRICTED", what + " is restricted");
            case Copybook.RC_UNAVAILABLE:
                return ApiException.upstream("BEDROCK_EOD", "Bedrock unavailable, end of day batch running");
            default:
                return ApiException.upstream("BEDROCK_ABEND", "Bedrock abend " + h.getAbendCode());
        }
    }
}
