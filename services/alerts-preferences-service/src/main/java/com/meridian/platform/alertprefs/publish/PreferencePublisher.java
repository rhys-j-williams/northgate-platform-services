package com.meridian.platform.alertprefs.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes to Redpanda (Kafka in the bank, MSK cluster kaf-prd-02). Publish is best effort: the
 * database write has already committed by the time we get here and a broker outage must not fail
 * the customer's save. Beacon tolerates a stale cache for preferences-cache-ttl; audit-trail has
 * the history table to reconcile from. PLAT-1102 is the outbox pattern we keep not getting to.
 */
@Component
public class PreferencePublisher {

    private static final Logger log = LoggerFactory.getLogger(PreferencePublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final String topic;
    private final AtomicLong dropped = new AtomicLong();

    public PreferencePublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper,
                               @Value("${meridian.alertprefs.topic:alerts.preferences.changed.v1}") String topic) {
        this.kafka = kafka;
        this.mapper = mapper;
        this.topic = topic;
    }

    public void publish(PreferenceChangedEvent event) {
        String payload;
        try {
            payload = mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        try {
            kafka.send(topic, event.getCustomerId(), payload).addCallback(
                ok -> log.info("published preference change customer={} code={}", event.getCustomerId(), event.getAlertCode()),
                ex -> {
                    dropped.incrementAndGet();
                    log.warn("preference change publish failed customer={} code={} reason={}",
                        event.getCustomerId(), event.getAlertCode(), ex.getMessage());
                });
        } catch (RuntimeException e) {
            // KafkaTemplate throws synchronously when metadata cannot be fetched inside max.block.ms
            dropped.incrementAndGet();
            log.warn("preference change publish failed (broker unavailable) customer={} code={}",
                event.getCustomerId(), event.getAlertCode());
        }
    }

    public long droppedCount() {
        return dropped.get();
    }
}
