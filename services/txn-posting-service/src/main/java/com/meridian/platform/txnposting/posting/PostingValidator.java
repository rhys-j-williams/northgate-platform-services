package com.meridian.platform.txnposting.posting;

import com.fasterxml.jackson.databind.JsonNode;
import com.meridian.platform.common.error.ApiException;
import com.meridian.platform.common.fixtures.FixtureStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Business validation that runs before we go anywhere near Bedrock. Bedrock will refuse most of
 * these itself, but a Bedrock refusal costs an MQ round trip and shows up on the CICS abend
 * dashboard, and the mainframe team have asked us (repeatedly, PLAT-0733) to stop sending them.
 */
@Component
public class PostingValidator {

    static final String ROUTING_NUMBER = "021000000";

    private final FixtureStore fixtures;
    private final PostingRepository repository;
    private final long dailyDebitLimitMinor;

    public PostingValidator(FixtureStore fixtures, PostingRepository repository,
                            @Value("${meridian.posting.daily-debit-limit-minor:2500000}") long dailyDebitLimitMinor) {
        this.fixtures = fixtures;
        this.repository = repository;
        this.dailyDebitLimitMinor = dailyDebitLimitMinor;
    }

    public void validate(PostingRequest request) {
        if (request.getAmountMinor() <= 0) {
            throw ApiException.badRequest("POSTING_AMOUNT", "amount must be positive");
        }
        JsonNode account = fixtures.account(request.getAccountId())
            .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "unknown account " + request.getAccountId()));

        String status = account.path("status").asText("OPEN").toUpperCase(Locale.ROOT);
        if (!"OPEN".equals(status) && !"ACTIVE".equals(status)) {
            throw ApiException.conflict("ACCOUNT_NOT_OPEN", "account is " + status);
        }
        String routing = account.path("routingNumber").asText(ROUTING_NUMBER);
        if (!ROUTING_NUMBER.equals(routing)) {
            // Should be impossible; the fixture package guarantees this. Left in after the 2022
            // Aggregio incident where an external routing number leaked in through a payee record.
            throw ApiException.badRequest("ROUTING_FOREIGN", "account is not held at this institution");
        }
        if ("DEBIT".equals(request.getType())) {
            long today = repository.debitedSince(request.getAccountId(), Instant.now().minus(Duration.ofHours(24)));
            if (today + request.getAmountMinor() > dailyDebitLimitMinor) {
                throw ApiException.conflict("DAILY_LIMIT", "daily debit limit exceeded");
            }
        }
    }
}
