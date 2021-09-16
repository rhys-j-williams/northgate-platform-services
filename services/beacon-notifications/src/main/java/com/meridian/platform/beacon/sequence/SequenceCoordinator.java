package com.meridian.platform.beacon.sequence;

import com.meridian.platform.beacon.event.AccountEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Per-customer ordering. Events carry a producer-assigned sequence; we only release an event when
 * every lower sequence for that customer has been dispatched (or given up on). Out of order
 * arrivals are parked in a per-customer TreeMap and drained as the gap closes.
 *
 * <p>Gaps do happen: the Bedrock batch bridge skips sequences for events it filters out, and
 * txn-posting reuses a sequence when it retries a failed post (PLAT-1902, not fixed on their side).
 * After {@code meridian.beacon.gap-timeout} the parked events are released anyway, in order, and
 * the gap is logged with the sequence numbers so Ops can reconcile against BEACON_CUSTOMER_SEQ.
 *
 * <p>Known issues:
 * <ul>
 *   <li>The parked map is in memory. A restart with parked events loses them (INC0046277 was
 *       partly this). PLAT-2210 is meant to move it to the database.</li>
 *   <li>A sequence lower than LAST_DISPATCHED is treated as a replay and released immediately,
 *       which is right for MQ redelivery and wrong for the PLAT-1902 reuse case.</li>
 *   <li>Nothing in here is covered by a test. The 2022 attempt (BeaconOrderingTest) was deleted in
 *       the 5.0 rewrite because it depended on the old Kafka layout and nobody rewrote it.</li>
 * </ul>
 */
@Component
public class SequenceCoordinator {

    private static final Logger log = LoggerFactory.getLogger(SequenceCoordinator.class);

    private final CustomerSequenceRepository repository;
    private final Duration gapTimeout;
    private final Map<String, NavigableMap<Long, Parked>> parked = new ConcurrentHashMap<>();

    public SequenceCoordinator(CustomerSequenceRepository repository,
                               @Value("${meridian.beacon.gap-timeout:PT30S}") Duration gapTimeout) {
        this.repository = repository;
        this.gapTimeout = gapTimeout;
    }

    /**
     * Returns the events that are now releasable for this customer, in sequence order. Usually a
     * list of one; empty when the event is parked; longer when the arrival closes a gap.
     */
    public synchronized List<AccountEvent> release(AccountEvent incoming) {
        String customerId = incoming.getCustomerId();
        long last = lastDispatched(customerId);
        long seq = incoming.getSequence();

        if (seq <= last) {
            log.info("customer {} sequence {} <= last dispatched {}, treating as replay", customerId, seq, last);
            return Collections.singletonList(incoming);
        }

        NavigableMap<Long, Parked> queue = parked.computeIfAbsent(customerId, k -> new TreeMap<>());
        queue.put(seq, new Parked(incoming, Instant.now()));

        List<AccountEvent> ready = new ArrayList<>();
        long expected = last + 1;
        while (!queue.isEmpty() && queue.firstKey() == expected) {
            ready.add(queue.pollFirstEntry().getValue().event);
            expected++;
        }
        if (ready.isEmpty()) {
            log.info("customer {} sequence {} parked, waiting for {}", customerId, seq, expected);
        }
        if (queue.isEmpty()) {
            parked.remove(customerId);
        }
        return ready;
    }

    /** Called after each event is fully processed; advances the persisted watermark. */
    public void markDispatched(String customerId, long sequence) {
        CustomerSequence row = repository.findById(customerId).orElseGet(() -> {
            CustomerSequence fresh = new CustomerSequence();
            fresh.setCustomerId(customerId);
            return fresh;
        });
        if (sequence > row.getLastDispatched()) {
            row.setLastDispatched(sequence);
            repository.save(row);
        }
    }

    /**
     * Gap timeout sweep. Any customer whose oldest parked event has waited longer than the timeout
     * has its watermark forced forward to just below that event, which makes the next
     * {@link #release} drain it. The forced-forward is logged as a gap.
     */
    @Scheduled(fixedDelayString = "${meridian.beacon.gap-sweep-ms:5000}")
    public synchronized void sweepGaps() {
        Instant cutoff = Instant.now().minus(gapTimeout);
        for (Map.Entry<String, NavigableMap<Long, Parked>> entry : parked.entrySet()) {
            NavigableMap<Long, Parked> queue = entry.getValue();
            if (queue.isEmpty()) {
                continue;
            }
            Map.Entry<Long, Parked> oldest = queue.firstEntry();
            if (oldest.getValue().parkedAt.isBefore(cutoff)) {
                long last = lastDispatched(entry.getKey());
                log.warn("customer {} gap {}..{} timed out after {}, forcing watermark", entry.getKey(),
                    last + 1, oldest.getKey() - 1, gapTimeout);
                markDispatched(entry.getKey(), oldest.getKey() - 1);
            }
        }
    }

    public Map<String, Integer> parkedDepth() {
        Map<String, Integer> depth = new TreeMap<>();
        parked.forEach((k, v) -> depth.put(k, v.size()));
        return depth;
    }

    private long lastDispatched(String customerId) {
        return repository.findById(customerId).map(CustomerSequence::getLastDispatched).orElse(0L);
    }

    private static final class Parked {
        private final AccountEvent event;
        private final Instant parkedAt;

        private Parked(AccountEvent event, Instant parkedAt) {
            this.event = event;
            this.parkedAt = parkedAt;
        }
    }
}
