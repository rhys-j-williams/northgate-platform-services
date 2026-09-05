package com.meridian.platform.common.correlation;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * Correlation id handling. The header name is fixed across the estate (retail-web's interceptor
 * generates it, the BFFs forward it, Bedrock adapter puts it in the MQ correlation id) so that a
 * single search in Splunk shows a request end to end. Do not rename it without talking to every
 * consumer; see INC0051230 for what happened last time someone did.
 */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationId() {
    }

    public static String current() {
        String value = MDC.get(MDC_KEY);
        return value == null ? generate() : value;
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static void bind(String value) {
        MDC.put(MDC_KEY, value);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
