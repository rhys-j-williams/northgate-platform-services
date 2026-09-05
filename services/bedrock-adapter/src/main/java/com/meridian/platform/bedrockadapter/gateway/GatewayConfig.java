package com.meridian.platform.bedrockadapter.gateway;

import com.meridian.platform.bedrockadapter.fixture.FixtureBedrock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;

/**
 * meridian.bedrock.mode:
 *   mq        real request/reply over MQ (default in every deployed environment)
 *   fixture   answer from the in-process fixture Bedrock, never touch the broker
 *   auto      try the broker, fall back to fixtures when it does not answer. Local default.
 *
 * meridian.bedrock.transport:
 *   mq        JMS (IBM MQ, or Artemis under the local-artemis profile)
 *   http      the bedrock-core-mock REST queue facade at meridian.bedrock.core-url. Laptops and
 *             build agents without the MQ image. See HttpBedrockGateway.
 *
 * "auto" exists so the estate can be brought up service by service without the mock-external
 * stack. It must never be set in a deployed environment; the Helm values hard code "mq".
 */
@Configuration
public class GatewayConfig {

    private static final Logger LOG = LoggerFactory.getLogger(GatewayConfig.class);

    @Bean
    @Primary
    public BedrockGateway bedrockGateway(JmsTemplate jmsTemplate,
                                         FixtureBedrock fixtureBedrock,
                                         @Value("${meridian.bedrock.mode:auto}") String mode,
                                         @Value("${meridian.bedrock.transport:mq}") String transport,
                                         @Value("${meridian.bedrock.core-url:http://localhost:4600}") String coreUrl,
                                         @Value("${meridian.mq.receive-timeout-ms:5000}") long receiveTimeoutMs,
                                         @Value("${meridian.bedrock.request-queue:BEDROCK.REQ}") String req,
                                         @Value("${meridian.bedrock.reply-queue:BEDROCK.RESP}") String resp) {
        BedrockGateway mq;
        if ("http".equals(transport)) {
            LOG.info("bedrock transport=http, bridging MTBREQ to {}", coreUrl);
            mq = new HttpBedrockGateway(coreUrl, req, resp, receiveTimeoutMs, fixtureBedrock);
        } else {
            mq = new MqBedrockGateway(jmsTemplate, req, resp);
        }
        switch (mode) {
            case "mq":
                return mq;
            case "fixture":
                LOG.warn("bedrock mode=fixture, no MQ traffic will be sent");
                return fixtureBedrock;
            case "auto":
            default:
                return new FallbackGateway(mq, fixtureBedrock);
        }
    }

    static final class FallbackGateway implements BedrockGateway {
        private final BedrockGateway primary;
        private final BedrockGateway fallback;
        private volatile boolean degraded;

        FallbackGateway(BedrockGateway primary, BedrockGateway fallback) {
            this.primary = primary;
            this.fallback = fallback;
        }

        @Override
        public String call(String tranCode, String request) {
            if (degraded) {
                return fallback.call(tranCode, request);
            }
            try {
                return primary.call(tranCode, request);
            } catch (BedrockUnavailableException e) {
                // Sticky. Once MQ has failed we stay on fixtures until restart; flapping between
                // real and synthetic balances mid demo is worse than either.
                LOG.warn("Bedrock over MQ unavailable, degrading to fixture responses: {}", e.getMessage());
                degraded = true;
                return fallback.call(tranCode, request);
            }
        }

        boolean isDegraded() {
            return degraded;
        }
    }
}
