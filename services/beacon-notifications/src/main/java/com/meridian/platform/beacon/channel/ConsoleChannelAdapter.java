package com.meridian.platform.beacon.channel;

import com.meridian.platform.beacon.event.AccountEvent;
import com.meridian.platform.beacon.notification.Notification;
import com.meridian.platform.beacon.preference.PreferenceDecision;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Debug tee. Every event that gets through the sequence coordinator is recorded here in the order
 * it was processed, whatever the preference decision was, so the estate smoke test (and anyone on
 * call at 3am) can see what Beacon did without grepping Splunk. Bounded ring, never persisted.
 *
 * <p>Also registered as a real channel ("CONSOLE") because the Ops replay tooling likes to force a
 * dispatch onto it. Nothing in production preferences ever selects it.
 */
@Component
public class ConsoleChannelAdapter extends LoggingChannelAdapter {

    public static final int CAPACITY = 2000;

    private final ConcurrentLinkedDeque<Map<String, Object>> ring = new ConcurrentLinkedDeque<>();

    @Override
    public String channel() {
        return "CONSOLE";
    }

    public void trace(AccountEvent event, PreferenceDecision decision, List<Notification> produced) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("eventId", event.getEventId());
        row.put("customerId", event.getCustomerId());
        row.put("accountId", event.getAccountId());
        row.put("sequence", event.getSequence());
        row.put("eventType", event.getEventType().name());
        row.put("channel", "console");
        row.put("decision", decision.shouldNotify() ? "NOTIFY" : "SUPPRESS");
        row.put("reason", decision.reason());
        row.put("channels", decision.shouldNotify() ? decision.channels() : Collections.emptyList());
        row.put("notifications", produced.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("notificationId", n.getNotificationId());
            m.put("channel", n.getChannel());
            m.put("status", n.getStatus().name());
            return m;
        }).collect(Collectors.toList()));
        row.put("processedAt", Instant.now());
        ring.addLast(row);
        while (ring.size() > CAPACITY) {
            ring.pollFirst();
        }
    }

    /** Oldest first, i.e. processing order, which for one customer is sequence order. */
    public List<Map<String, Object>> dispatches(String customerId, int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        Iterator<Map<String, Object>> it = ring.iterator();
        while (it.hasNext()) {
            Map<String, Object> row = it.next();
            if (customerId == null || customerId.equals(row.get("customerId"))) {
                out.add(row);
            }
        }
        if (out.size() > limit) {
            return new ArrayList<>(out.subList(out.size() - limit, out.size()));
        }
        return out;
    }

    public void clear() {
        ring.clear();
    }
}
