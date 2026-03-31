package com.salygin.ssubench.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestContextFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestContextFilter.class);

    private static final String HEADER_CORRELATION_ID = "X-Request-Id";
    private static final String MDC_KEY_PRIMARY = "tracking_id";
    private static final String MDC_KEY_ALTERNATE = "trackingId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String correlationId = obtainCorrelationId(req);
        long startTime = System.currentTimeMillis();

        attachToMdc(correlationId);
        res.setHeader(HEADER_CORRELATION_ID, correlationId);

        String uriWithQuery = buildUri(req);

        logger.info(
                "request started id={} method={} uri={}",
                correlationId,
                req.getMethod(),
                uriWithQuery
        );

        try {
            chain.doFilter(req, res);
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;

            logger.info(
                    "request finished id={} method={} uri={} status={} timeMs={}",
                    correlationId,
                    req.getMethod(),
                    uriWithQuery,
                    res.getStatus(),
                    elapsed
            );

            clearMdc();
        }
    }

    private String obtainCorrelationId(HttpServletRequest req) {
        String headerValue = req.getHeader(HEADER_CORRELATION_ID);

        if (headerValue != null && !headerValue.trim().isEmpty()) {
            return headerValue.strip();
        }

        return UUID.randomUUID().toString();
    }

    private void attachToMdc(String id) {
        MDC.put(MDC_KEY_PRIMARY, id);
        MDC.put(MDC_KEY_ALTERNATE, id);
    }

    private void clearMdc() {
        MDC.remove(MDC_KEY_PRIMARY);
        MDC.remove(MDC_KEY_ALTERNATE);
    }

    private String buildUri(HttpServletRequest req) {
        String qs = req.getQueryString();
        String base = req.getRequestURI();

        return (qs == null || qs.isBlank()) ? base : base + "?" + qs;
    }
}
