package com.meridian.platform.common.correlation;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Propagates the current correlation id on outbound RestTemplate calls. */
public class CorrelationIdRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        if (!request.getHeaders().containsKey(CorrelationId.HEADER)) {
            request.getHeaders().add(CorrelationId.HEADER, CorrelationId.current());
        }
        return execution.execute(request, body);
    }
}
