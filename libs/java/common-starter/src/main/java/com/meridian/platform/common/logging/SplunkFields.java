package com.meridian.platform.common.logging;

import org.slf4j.MDC;

/**
 * Standard Splunk field names. The dashboards in the Beacon and Payments Splunk apps are built on
 * these exact keys, so a rename here is a dashboard change there.
 */
public final class SplunkFields {

    public static final String EVENT = "event";
    public static final String SEVERITY = "severity";
    public static final String CORRELATION_ID = "correlationId";
    public static final String CUSTOMER_ID = "customerId";
    public static final String SERVICE = "service";
    public static final String ENVIRONMENT = "environment";

    private SplunkFields() {
    }

    /** Puts a business key on the MDC for the duration of a unit of work. Never a PII value. */
    public static AutoCloseable with(String key, String value) {
        MDC.put(key, value);
        return () -> MDC.remove(key);
    }
}
