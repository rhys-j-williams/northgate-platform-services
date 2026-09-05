package com.meridian.platform.txnposting.posting;

import com.meridian.platform.common.correlation.CorrelationId;
import com.meridian.platform.common.error.ApiException;
import com.meridian.platform.txnposting.bedrock.BedrockAdapterClient;
import com.meridian.platform.txnposting.bedrock.BedrockPostingResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posting flow: idempotency lookup -> validate -> Bedrock -> record. Reversals come in through
 * {@link #reverse}. Both write a POSTING row whatever the outcome so the 05:30 reconciliation has
 * something to reconcile.
 *
 * Compliance note (SOX control TP-04): nothing in here updates or deletes a POSTING row other than
 * the status flip to REVERSED and the pending sweep. Do not add an update path without talking to
 * Controls first.
 */
@Service
public class PostingService {

    private static final Logger log = LoggerFactory.getLogger(PostingService.class);

    public static final String POSTED = "POSTED";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String REFUSED = "REFUSED";
    public static final String PENDING_BEDROCK = "PENDING_BEDROCK";
    public static final String REVERSED = "REVERSED";

    private final PostingRepository repository;
    private final PostingValidator validator;
    private final BedrockAdapterClient bedrock;
    private final boolean holdWhenUnavailable;
    // TODO PLAT-1301 read from POSTING_PARAM instead of duplicating the batch's value here
    private final int reversalWindowDays;

    public PostingService(PostingRepository repository, PostingValidator validator, BedrockAdapterClient bedrock,
                          @Value("${meridian.posting.hold-when-bedrock-unavailable:true}") boolean holdWhenUnavailable,
                          @Value("${meridian.posting.reversal-window-days:60}") int reversalWindowDays) {
        this.repository = repository;
        this.validator = validator;
        this.bedrock = bedrock;
        this.holdWhenUnavailable = holdWhenUnavailable;
        this.reversalWindowDays = reversalWindowDays;
    }

    @Transactional
    public PostingOutcome post(String idempotencyKey, PostingRequest request, String channel) {
        if (!IdempotencyKeys.wellFormed(idempotencyKey)) {
            throw ApiException.badRequest("IDEMPOTENCY_KEY", "Idempotency-Key header missing or malformed");
        }
        String hash = IdempotencyKeys.fingerprint(request);
        Optional<Posting> existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Posting prior = existing.get();
            if (!prior.getRequestHash().equals(hash)) {
                throw ApiException.conflict("IDEMPOTENCY_MISMATCH",
                    "Idempotency-Key was already used for a different posting");
            }
            return new PostingOutcome(prior, true);
        }

        validator.validate(request);

        Posting posting = new Posting();
        posting.setIdempotencyKey(idempotencyKey);
        posting.setRequestHash(hash);
        posting.setAccountId(request.getAccountId());
        posting.setType(request.getType());
        posting.setAmountMinor(request.getAmountMinor());
        posting.setDescription(request.getDescription());
        posting.setChannel(channel);
        posting.setCorrelationId(CorrelationId.current());

        BedrockPostingResult result = bedrock.post(idempotencyKey, request.getType().charAt(0), request.getAccountId(),
            request.getAmountMinor(), null, request.getDescription(), channel);
        apply(posting, result);
        Posting saved = repository.save(posting);
        log.info("posting {} {} {} {} -> {}", saved.getId(), saved.getType(), saved.getAccountId(),
            saved.getAmountMinor(), saved.getStatus());
        return new PostingOutcome(saved, false);
    }

    @Transactional
    public PostingOutcome reverse(long postingId, String idempotencyKey, String channel) {
        if (!IdempotencyKeys.wellFormed(idempotencyKey)) {
            throw ApiException.badRequest("IDEMPOTENCY_KEY", "Idempotency-Key header missing or malformed");
        }
        Optional<Posting> replay = repository.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) {
            return new PostingOutcome(replay.get(), true);
        }
        Posting original = repository.findById(postingId)
            .orElseThrow(() -> ApiException.notFound("POSTING_NOT_FOUND", "no posting " + postingId));
        if (!POSTED.equals(original.getStatus())) {
            throw ApiException.conflict("REVERSAL_NOT_POSTED", "only POSTED transactions can be reversed");
        }
        if (original.getCreatedAt().isBefore(Instant.now().minus(Duration.ofDays(reversalWindowDays)))) {
            throw ApiException.conflict("REVERSAL_WINDOW", "outside the " + reversalWindowDays + " day reversal window");
        }

        Posting reversal = new Posting();
        reversal.setIdempotencyKey(idempotencyKey);
        reversal.setRequestHash("REVERSAL:" + original.getId());
        reversal.setAccountId(original.getAccountId());
        reversal.setType("REVERSAL");
        reversal.setAmountMinor(original.getAmountMinor());
        reversal.setDescription("REV " + original.getBedrockTransactionId());
        reversal.setChannel(channel);
        reversal.setOriginalPostingId(original.getId());
        reversal.setCorrelationId(CorrelationId.current());

        BedrockPostingResult result = bedrock.post(idempotencyKey, 'R', original.getAccountId(),
            original.getAmountMinor(), original.getBedrockTransactionId(), reversal.getDescription(), channel);
        apply(reversal, result);
        Posting saved = repository.save(reversal);
        if (POSTED.equals(saved.getStatus())) {
            original.setStatus(REVERSED);
            original.setReversedById(saved.getId());
            repository.save(original);
        }
        return new PostingOutcome(saved, false);
    }

    private void apply(Posting posting, BedrockPostingResult result) {
        switch (result.getOutcome()) {
            case POSTED:
                posting.setStatus(POSTED);
                posting.setBedrockTransactionId(result.getTransactionId());
                posting.setNewBalanceMinor(result.getNewBalanceMinor());
                break;
            case DUPLICATE:
                // Bedrock has seen this key already but we have no row: probably the previous
                // attempt died between the MQ reply and the commit. Record it as a duplicate.
                posting.setStatus(DUPLICATE);
                posting.setBedrockTransactionId(result.getTransactionId());
                posting.setNewBalanceMinor(result.getNewBalanceMinor());
                break;
            case REFUSED:
                posting.setStatus(REFUSED);
                posting.setRefusalReason(result.getReason());
                break;
            case UNAVAILABLE:
            default:
                if (!holdWhenUnavailable || "REVERSAL".equals(posting.getType())) {
                    throw ApiException.upstream("BEDROCK_UNAVAILABLE", "Bedrock is unavailable: " + result.getReason());
                }
                posting.setStatus(PENDING_BEDROCK);
                posting.setRefusalReason(result.getReason());
                break;
        }
    }

    public Posting get(long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("POSTING_NOT_FOUND", "no posting " + id));
    }

    public List<Posting> recentForAccount(String accountId) {
        return repository.findTop50ByAccountIdOrderByCreatedAtDesc(accountId);
    }

    public List<Posting> pending() {
        return repository.findByStatusOrderByCreatedAtAsc(PENDING_BEDROCK);
    }

    public static final class PostingOutcome {
        private final Posting posting;
        private final boolean replayed;

        PostingOutcome(Posting posting, boolean replayed) {
            this.posting = posting;
            this.replayed = replayed;
        }

        public Posting getPosting() { return posting; }
        public boolean isReplayed() { return replayed; }
    }
}
