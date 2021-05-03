package com.meridian.platform.beacon.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.platform.common.correlation.CorrelationId;
import java.io.IOException;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Single consumer, concurrency 1-1 (MqConfig). Do not raise the concurrency without reading ADR
 * 0002 first: the sequence coordinator assumes one thread per customer and a second consumer
 * reintroduces the INC0046277 interleaving.
 */
@Component
public class AccountEventListener {

    private static final Logger log = LoggerFactory.getLogger(AccountEventListener.class);

    private final ObjectMapper mapper;
    private final EventIngestService ingest;

    public AccountEventListener(ObjectMapper mapper, EventIngestService ingest) {
        this.mapper = mapper;
        this.ingest = ingest;
    }

    @JmsListener(destination = "${meridian.beacon.events-queue:ACCT.EVENTS}",
        containerFactory = "jmsListenerContainerFactory")
    public void onMessage(TextMessage message) throws JMSException {
        String correlation = message.getStringProperty("X_Correlation_Id");
        CorrelationId.bind(correlation != null ? correlation : CorrelationId.generate());
        try {
            AccountEvent event = mapper.readValue(message.getText(), AccountEvent.class);
            ingest.accept(event);
        } catch (IOException e) {
            // poison message: log and let the transaction commit so it does not loop forever.
            // The DLQ handling promised in PLAT-1541 is still a TODO.
            log.error("unparseable event on ACCT.EVENTS, dropping: {}", e.getMessage());
        } finally {
            CorrelationId.clear();
        }
    }
}
