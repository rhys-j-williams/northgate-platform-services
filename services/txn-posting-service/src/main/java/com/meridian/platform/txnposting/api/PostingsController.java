package com.meridian.platform.txnposting.api;

import com.meridian.platform.txnposting.posting.Posting;
import com.meridian.platform.txnposting.posting.PostingRequest;
import com.meridian.platform.txnposting.posting.PostingService;
import com.meridian.platform.txnposting.posting.PostingService.PostingOutcome;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/postings/v1")
public class PostingsController {

    private final PostingService service;

    public PostingsController(PostingService service) {
        this.service = service;
    }

    @PostMapping("/postings")
    public ResponseEntity<PostingView> post(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                            @RequestHeader(value = "X-Channel", defaultValue = "RTL") String channel,
                                            @Valid @RequestBody PostingRequest body) {
        PostingOutcome outcome = service.post(idempotencyKey, body, channel);
        return respond(outcome);
    }

    @PostMapping("/postings/{postingId}/reversals")
    public ResponseEntity<PostingView> reverse(@PathVariable long postingId,
                                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                               @RequestHeader(value = "X-Channel", defaultValue = "RTL") String channel) {
        return respond(service.reverse(postingId, idempotencyKey, channel));
    }

    @GetMapping("/postings/{postingId}")
    public PostingView get(@PathVariable long postingId) {
        return PostingView.of(service.get(postingId), false);
    }

    @GetMapping("/accounts/{accountId}/postings")
    public List<PostingView> forAccount(@PathVariable String accountId) {
        return service.recentForAccount(accountId).stream().map(p -> PostingView.of(p, false)).collect(Collectors.toList());
    }

    @GetMapping("/pending")
    public List<PostingView> pending() {
        return service.pending().stream().map(p -> PostingView.of(p, false)).collect(Collectors.toList());
    }

    private static ResponseEntity<PostingView> respond(PostingOutcome outcome) {
        Posting p = outcome.getPosting();
        HttpStatus status;
        if (outcome.isReplayed() || PostingService.DUPLICATE.equals(p.getStatus())) {
            status = HttpStatus.OK;
        } else if (PostingService.PENDING_BEDROCK.equals(p.getStatus())) {
            status = HttpStatus.ACCEPTED;
        } else if (PostingService.REFUSED.equals(p.getStatus())) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
        } else {
            status = HttpStatus.CREATED;
        }
        return ResponseEntity.status(status).header("Idempotent-Replayed", String.valueOf(outcome.isReplayed()))
            .body(PostingView.of(p, outcome.isReplayed()));
    }
}
