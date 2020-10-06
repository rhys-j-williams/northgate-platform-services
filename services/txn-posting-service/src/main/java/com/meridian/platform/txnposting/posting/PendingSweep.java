package com.meridian.platform.txnposting.posting;

import com.meridian.platform.txnposting.bedrock.BedrockAdapterClient;
import com.meridian.platform.txnposting.bedrock.BedrockPostingResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replays PENDING_BEDROCK rows once Bedrock is back. In the bank this is a Control-M job at 05:30
 * after EOD; locally it is a fixed-delay schedule so the demo does not have to wait until morning.
 */
@Component
public class PendingSweep {

    private static final Logger log = LoggerFactory.getLogger(PendingSweep.class);

    private final PostingRepository repository;
    private final BedrockAdapterClient bedrock;

    public PendingSweep(PostingRepository repository, BedrockAdapterClient bedrock) {
        this.repository = repository;
        this.bedrock = bedrock;
    }

    @Scheduled(fixedDelayString = "${meridian.posting.pending-sweep-ms:60000}", initialDelayString = "30000")
    @Transactional
    public int sweep() {
        List<Posting> pending = repository.findByStatusOrderByCreatedAtAsc(PostingService.PENDING_BEDROCK);
        int replayed = 0;
        for (Posting p : pending) {
            BedrockPostingResult result = bedrock.post(p.getIdempotencyKey(), p.getType().charAt(0), p.getAccountId(),
                p.getAmountMinor(), null, p.getDescription(), p.getChannel());
            if (result.getOutcome() == BedrockPostingResult.Outcome.UNAVAILABLE) {
                break; // still down, try again next time
            }
            p.setStatus(result.getOutcome() == BedrockPostingResult.Outcome.REFUSED
                ? PostingService.REFUSED : PostingService.POSTED);
            p.setBedrockTransactionId(result.getTransactionId());
            p.setNewBalanceMinor(result.getNewBalanceMinor());
            p.setRefusalReason(result.getReason());
            repository.save(p);
            replayed++;
        }
        if (!pending.isEmpty()) {
            log.info("pending sweep replayed {} of {}", replayed, pending.size());
        }
        return replayed;
    }
}
