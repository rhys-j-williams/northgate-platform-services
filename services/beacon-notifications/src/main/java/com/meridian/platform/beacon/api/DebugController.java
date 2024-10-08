package com.meridian.platform.beacon.api;

import com.meridian.platform.beacon.channel.ConsoleChannelAdapter;
import com.meridian.platform.beacon.event.AccountEvent;
import com.meridian.platform.beacon.event.EventIngestService;
import com.meridian.platform.beacon.event.EventType;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local-only side door used by mock-external/smoke.sh and estate-up.sh. Takes the loosely typed
 * ACCT.EVENTS shape the Bedrock bridge emits (eventType TRANSACTION_POSTED, no balanceAfter) and
 * coerces it into what {@link EventIngestService} wants. Not registered outside the local profile;
 * the OpenShift route does not expose /debug either way (PLAT-2210).
 */
@RestController
@RequestMapping("/debug")
@Profile("!prod & !uat")
public class DebugController {

    private final EventIngestService ingest;
    private final ConsoleChannelAdapter console;

    public DebugController(EventIngestService ingest, ConsoleChannelAdapter console) {
        this.ingest = ingest;
        this.console = console;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingest(@RequestBody Map<String, Object> raw) {
        AccountEvent event = coerce(raw);
        int released = ingest.accept(event);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accepted", event.getEventId());
        body.put("eventType", event.getEventType().name());
        body.put("released", released);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    @GetMapping("/dispatches")
    public List<Map<String, Object>> dispatches(@RequestParam(required = false) String customerId,
                                                @RequestParam(required = false) String channel,
                                                @RequestParam(defaultValue = "100") int limit) {
        // channel is accepted for symmetry with the real adapters; the console tee sees everything
        return console.dispatches(customerId, Math.max(1, Math.min(limit, ConsoleChannelAdapter.CAPACITY)));
    }

    @DeleteMapping("/dispatches")
    public ResponseEntity<Void> clear() {
        console.clear();
        return ResponseEntity.noContent().build();
    }

    static AccountEvent coerce(Map<String, Object> raw) {
        AccountEvent e = new AccountEvent();
        e.setEventId(str(raw, "eventId"));
        e.setCustomerId(str(raw, "customerId"));
        e.setAccountId(str(raw, "accountId"));
        e.setSequence(num(raw, "sequence"));
        e.setAmountMinor(num(raw, "amountMinor"));
        e.setBalanceAfterMinor(num(raw, "balanceAfterMinor"));
        e.setDescription(str(raw, "description"));
        e.setChannel(str(raw, "channel"));
        String at = str(raw, "occurredAt");
        e.setOccurredAt(at == null ? Instant.now() : Instant.parse(at));
        e.setEventType(mapType(str(raw, "eventType"), e.getAmountMinor()));
        return e;
    }

    /** The bridge says TRANSACTION_POSTED; we say LARGE_DEBIT / LARGE_CREDIT. Unknown types are treated the same way. */
    static EventType mapType(String type, long amountMinor) {
        if (type != null) {
            for (EventType t : EventType.values()) {
                if (t.name().equalsIgnoreCase(type)) {
                    return t;
                }
            }
        }
        return amountMinor < 0 ? EventType.LARGE_DEBIT : EventType.LARGE_CREDIT;
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }

    private static long num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (v == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
