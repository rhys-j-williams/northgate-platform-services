package com.meridian.platform.txnposting.posting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meridian.platform.common.error.ApiException;
import com.meridian.platform.common.fixtures.FixtureStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostingValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private PostingRepository repository;
    private PostingValidator validator;

    @BeforeEach
    void setUp() {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode acct = root.putArray("accounts").addObject();
        acct.put("accountId", "ACC-200001").put("status", "OPEN").put("routingNumber", "021000000");
        ObjectNode closed = root.withArray("accounts").addObject();
        closed.put("accountId", "ACC-200002").put("status", "CLOSED").put("routingNumber", "021000000");
        repository = mock(PostingRepository.class);
        when(repository.debitedSince(any(), any())).thenReturn(0L);
        validator = new PostingValidator(new FixtureStore(root), repository, 2_500_000);
    }

    private static PostingRequest req(String acct, String type, long amt) {
        PostingRequest r = new PostingRequest();
        r.setAccountId(acct);
        r.setType(type);
        r.setAmountMinor(amt);
        return r;
    }

    @Test
    void acceptsOpenAccountCredit() {
        validator.validate(req("ACC-200001", "CREDIT", 12_50));
    }

    @Test
    void rejectsUnknownAccount() {
        assertThatThrownBy(() -> validator.validate(req("ACC-999999", "CREDIT", 100)))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void rejectsClosedAccount() {
        assertThatThrownBy(() -> validator.validate(req("ACC-200002", "DEBIT", 100)))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("ACCOUNT_NOT_OPEN"));
    }

    @Test
    void rejectsDebitOverDailyLimit() {
        when(repository.debitedSince(eq("ACC-200001"), any())).thenReturn(2_400_000L);
        assertThatThrownBy(() -> validator.validate(req("ACC-200001", "DEBIT", 200_000)))
            .isInstanceOf(ApiException.class)
            .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("DAILY_LIMIT"));
    }

    // TODO PLAT-1420 reversal window and idempotency mismatch cases. Blocked on a Bedrock stub that
    // can return RC 20; the adapter's fixture mode always returns fresh transaction ids.
}
