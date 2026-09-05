package com.meridian.platform.entitlements.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.platform.entitlements.entitlement.Entitlement;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Best effort publication to entitlements.changed.v1, keyed by organisation. audit-trail-service
 * consumes it. If Redpanda is down we log and carry on: the database is the record, the topic
 * is a notification. Same stance as alerts-preferences (PLAT-1099).
 */
@Component
public class EntitlementEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(EntitlementEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final String topic;

    public EntitlementEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper,
                                     @Value("${meridian.events.topic:entitlements.changed.v1}") String topic) {
        this.kafka = kafka;
        this.mapper = mapper;
        this.topic = topic;
    }

    public void granted(Entitlement e, String by, String approvedBy) {
        publish(new EntitlementChangedEvent("GRANTED", e.getEntitlementId(), e.getOrganisationId(), e.getUserHandle(),
            e.getRoleCode(), by, approvedBy, Instant.now()));
    }

    public void revoked(Entitlement e, String by, String approvedBy) {
        publish(new EntitlementChangedEvent("REVOKED", e.getEntitlementId(), e.getOrganisationId(), e.getUserHandle(),
            e.getRoleCode(), by, approvedBy, Instant.now()));
    }

    void publish(EntitlementChangedEvent event) {
        try {
            String json = mapper.writeValueAsString(event);
            kafka.send(topic, event.organisationId(), json).get(2500, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOG.warn("entitlement_event_publish_failed type={} entitlementId={} reason={}",
                event.eventType(), event.entitlementId(), ex.getClass().getSimpleName());
        }
    }
}
