package com.meridian.platform.txnposting.bedrock;

import com.fasterxml.jackson.databind.JsonNode;
import com.meridian.platform.common.correlation.CorrelationId;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin client over bedrock-adapter's /bedrock/v1/postings. We never talk MQ from here; that was the
 * whole point of the adapter (ADR-0002). Timeouts are short because the adapter itself waits up to
 * 8s on the reply queue and the BFF gives us 12s in total.
 */
@Component
public class BedrockAdapterClient {

    private static final Logger log = LoggerFactory.getLogger(BedrockAdapterClient.class);

    private final RestTemplate http;
    private final String baseUrl;

    public BedrockAdapterClient(RestTemplateBuilder builder,
                                @Value("${meridian.posting.bedrock-adapter-url:http://localhost:4516/bedrock/v1}") String baseUrl) {
        this.http = builder.setConnectTimeout(Duration.ofMillis(800)).setReadTimeout(Duration.ofSeconds(10)).build();
        this.baseUrl = baseUrl;
    }

    public BedrockPostingResult post(String idempotencyKey, char type, String accountId, long amountMinor,
                                     String originalTransactionId, String description, String channel) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotencyKey", idempotencyKey);
        body.put("type", type == 'R' ? "REVERSAL" : type == 'D' ? "DEBIT" : "CREDIT");
        body.put("accountId", accountId);
        body.put("amountMinor", amountMinor);
        if (originalTransactionId != null) {
            body.put("originalTransactionId", originalTransactionId);
        }
        // Bedrock's TXN-POST description field is PIC X(32); the adapter refuses longer.
        body.put("description", description == null ? "" : description.length() > 32 ? description.substring(0, 32) : description);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(CorrelationId.HEADER, CorrelationId.current());
        headers.set("X-Channel", channel);
        try {
            ResponseEntity<JsonNode> rsp = http.postForEntity(baseUrl + "/postings", new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode node = rsp.getBody();
            String rc = node == null ? "" : node.path("header").path("returnCode").asText();
            BedrockPostingResult.Outcome outcome = "20".equals(rc)
                ? BedrockPostingResult.Outcome.DUPLICATE : BedrockPostingResult.Outcome.POSTED;
            return new BedrockPostingResult(outcome,
                node == null ? null : node.path("transactionId").asText(null),
                node == null || node.path("newBalanceMinor").isNull() ? null : node.path("newBalanceMinor").asLong(),
                node == null ? null : node.path("reason").asText(null));
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 409) {
                return new BedrockPostingResult(BedrockPostingResult.Outcome.REFUSED, null, null, codeOf(e));
            }
            log.warn("bedrock-adapter returned {} for posting {}", e.getStatusCode().value(), idempotencyKey);
            return BedrockPostingResult.unavailable(codeOf(e));
        } catch (ResourceAccessException e) {
            log.warn("bedrock-adapter unreachable at {}: {}", baseUrl, e.getMessage());
            return BedrockPostingResult.unavailable("ADAPTER_UNREACHABLE");
        } catch (RestClientException e) {
            return BedrockPostingResult.unavailable("ADAPTER_ERROR");
        }
    }

    private static String codeOf(HttpStatusCodeException e) {
        String body = e.getResponseBodyAsString();
        int i = body.indexOf("\"code\":\"");
        if (i < 0) {
            return "HTTP_" + e.getStatusCode().value();
        }
        int end = body.indexOf('"', i + 8);
        return end < 0 ? "HTTP_" + e.getStatusCode().value() : body.substring(i + 8, end);
    }
}
