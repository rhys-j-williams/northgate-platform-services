package com.meridian.platform.common.correlation;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reads or mints the correlation id, binds it to the MDC and echoes it on the response. */
public class CorrelationIdFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger ACCESS = LoggerFactory.getLogger("http.access");

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(CorrelationId.HEADER);
        String id = incoming == null || incoming.isBlank() ? CorrelationId.generate() : incoming.trim();
        // A 200 character correlation id is somebody's stack trace, not an id. PLAT-3312.
        if (id.length() > 64) {
            id = id.substring(0, 64);
        }
        CorrelationId.bind(id);
        response.setHeader(CorrelationId.HEADER, id);
        long started = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            if (!request.getRequestURI().startsWith("/actuator")) {
                ACCESS.info("http.request method={} path={} status={} durationMs={}", request.getMethod(),
                        request.getRequestURI(), response.getStatus(), (System.nanoTime() - started) / 1_000_000);
            }
            CorrelationId.clear();
        }
    }
}
