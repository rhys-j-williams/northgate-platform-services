package com.meridian.platform.bedrockadapter.gateway;

import com.meridian.platform.common.correlation.CorrelationId;
import java.util.UUID;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;

/**
 * Request/reply over BEDROCK.REQ and BEDROCK.RESP using the JMS correlation id. Bedrock's CICS
 * bridge copies JMSCorrelationID from the request to the reply; we use a fresh UUID per call
 * rather than the platform correlation id because the same platform request may fan out into
 * several Bedrock calls (statement generation does exactly that) and the selector must be unique.
 *
 * Timeout is deliberately short. If Bedrock is in EOD batch it answers RC 12 quickly; if the
 * bridge is down, we would rather fail the caller than hold a Tomcat thread (INC0051230).
 */
public class MqBedrockGateway implements BedrockGateway {

    private static final Logger LOG = LoggerFactory.getLogger(MqBedrockGateway.class);

    private final JmsTemplate jms;
    private final String requestQueue;
    private final String replyQueue;

    public MqBedrockGateway(JmsTemplate jms,
                            @Value("${meridian.bedrock.request-queue:BEDROCK.REQ}") String requestQueue,
                            @Value("${meridian.bedrock.reply-queue:BEDROCK.RESP}") String replyQueue) {
        this.jms = jms;
        this.requestQueue = requestQueue;
        this.replyQueue = replyQueue;
    }

    @Override
    public String call(String tranCode, String request) {
        String mqCorrelation = UUID.randomUUID().toString().replace("-", "");
        try {
            jms.send(requestQueue, session -> {
                TextMessage m = session.createTextMessage(request);
                m.setJMSCorrelationID(mqCorrelation);
                m.setJMSReplyTo(session.createQueue(replyQueue));
                m.setStringProperty("MTB_TRAN_CODE", tranCode);
                m.setStringProperty("MTB_PLATFORM_CORRELATION", CorrelationId.current());
                return m;
            });
            Message reply = jms.receiveSelected(replyQueue, "JMSCorrelationID = '" + mqCorrelation + "'");
            if (reply == null) {
                throw new BedrockUnavailableException("no reply from Bedrock for " + tranCode
                    + " within " + jms.getReceiveTimeout() + "ms");
            }
            if (!(reply instanceof TextMessage)) {
                throw new BedrockUnavailableException("non text reply from Bedrock: " + reply.getClass().getSimpleName());
            }
            return ((TextMessage) reply).getText();
        } catch (JmsException | JMSException e) {
            LOG.warn("Bedrock MQ call failed tranCode={} queue={}", tranCode, requestQueue, e);
            throw new BedrockUnavailableException("Bedrock MQ call failed: " + e.getMessage(), e);
        }
    }
}
