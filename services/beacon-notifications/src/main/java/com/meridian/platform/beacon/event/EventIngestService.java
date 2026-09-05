package com.meridian.platform.beacon.event;

import com.meridian.platform.beacon.channel.ChannelDispatcher;
import com.meridian.platform.beacon.notification.Notification;
import com.meridian.platform.beacon.notification.NotificationRepository;
import com.meridian.platform.beacon.preference.PreferenceDecision;
import com.meridian.platform.beacon.preference.PreferenceEvaluator;
import com.meridian.platform.beacon.sequence.SequenceCoordinator;
import com.meridian.platform.beacon.template.RenderedTemplate;
import com.meridian.platform.beacon.template.TemplateRegistry;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The pipeline: order, evaluate, render, dispatch, persist. Ordering is the coordinator's job;
 * everything after {@link SequenceCoordinator#release} runs strictly in sequence for a customer.
 */
@Service
public class EventIngestService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestService.class);

    private final SequenceCoordinator coordinator;
    private final PreferenceEvaluator preferences;
    private final TemplateRegistry templates;
    private final ChannelDispatcher dispatcher;
    private final NotificationRepository repository;

    public EventIngestService(SequenceCoordinator coordinator, PreferenceEvaluator preferences,
                              TemplateRegistry templates, ChannelDispatcher dispatcher,
                              NotificationRepository repository) {
        this.coordinator = coordinator;
        this.preferences = preferences;
        this.templates = templates;
        this.dispatcher = dispatcher;
        this.repository = repository;
    }

    /** Accepts an event in any order and processes whatever is now releasable for that customer. */
    @Transactional
    public int accept(AccountEvent event) {
        List<AccountEvent> ready = coordinator.release(event);
        for (AccountEvent e : ready) {
            process(e);
        }
        return ready.size();
    }

    private void process(AccountEvent event) {
        if (repository.existsByEventId(event.getEventId())) {
            log.info("duplicate event {} ignored", event.getEventId());
            return;
        }
        PreferenceDecision decision = preferences.evaluate(event);
        if (!decision.shouldNotify()) {
            log.info("event {} suppressed for {}: {}", event.getEventId(), event.getCustomerId(), decision.reason());
            coordinator.markDispatched(event.getCustomerId(), event.getSequence());
            return;
        }
        RenderedTemplate rendered = templates.render(event, decision);
        for (String channel : decision.channels()) {
            Notification n = new Notification();
            n.setEventId(event.getEventId());
            n.setCustomerId(event.getCustomerId());
            n.setAccountId(event.getAccountId());
            n.setCustomerSequence(event.getSequence());
            n.setEventType(event.getEventType().name());
            n.setTemplateCode(rendered.getTemplateCode());
            n.setChannel(channel);
            n.setRegulatory(event.getEventType().isRegulatory());
            n.setRenderedSubject(rendered.getSubject());
            n.setRenderedBody(rendered.getBody());
            n.setOccurredAt(event.getOccurredAt());
            n.setStatus(Notification.Status.PENDING);
            n = repository.save(n);
            try {
                dispatcher.dispatch(channel, n);
                n.setStatus(Notification.Status.SENT);
                n.setDispatchedAt(Instant.now());
            } catch (RuntimeException ex) {
                n.setStatus(Notification.Status.FAILED);
                n.setFailureReason(ex.getMessage());
                n.setAttempts(n.getAttempts() + 1);
                log.warn("dispatch of {} on {} failed: {}", n.getEventId(), channel, ex.getMessage());
            }
            repository.save(n);
        }
        coordinator.markDispatched(event.getCustomerId(), event.getSequence());
    }
}
