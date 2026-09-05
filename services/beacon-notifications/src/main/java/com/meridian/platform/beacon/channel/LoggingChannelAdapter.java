package com.meridian.platform.beacon.channel;

import com.meridian.platform.beacon.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Base for the local adapters. Every real adapter in the bank (SMS via the aggregator, push via
 * the mobile gateway, email via the ESP relay, letter via the print house SFTP drop) is a
 * subclass that overrides {@link #deliver}. Locally they all just log, which is what the smoke
 * test and the Splunk HEC mock pick up.
 */
public abstract class LoggingChannelAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger("beacon.dispatch");

    @Override
    public void send(Notification n) {
        MDC.put("customerId", n.getCustomerId());
        MDC.put("sequence", Long.toString(n.getCustomerSequence()));
        try {
            deliver(n);
            log.info("BEACON DISPATCH channel={} customer={} seq={} template={} regulatory={} subject=\"{}\"",
                channel(), n.getCustomerId(), n.getCustomerSequence(), n.getTemplateCode(), n.isRegulatory(),
                n.getRenderedSubject());
        } finally {
            MDC.remove("customerId");
            MDC.remove("sequence");
        }
    }

    protected void deliver(Notification n) {
        // local: nothing leaves the box
    }
}
