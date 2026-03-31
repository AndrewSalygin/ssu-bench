package com.salygin.ssubench.security;

import com.salygin.ssubench.api.error.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JsonForbiddenHandler implements AccessDeniedHandler {

    private final ObjectMapper jsonMapper;

    @Override
    public void handle(
            HttpServletRequest req,
            HttpServletResponse res,
            AccessDeniedException ex
    ) throws IOException {

        int httpStatus = HttpServletResponse.SC_FORBIDDEN;
        String text = normalizeMessage(ex);

        res.setStatus(httpStatus);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiErrorResponse payload = new ApiErrorResponse(
                httpStatus,
                "Forbidden",
                text,
                req.getRequestURI(),
                resolveTrackingId()
        );

        jsonMapper.writeValue(res.getWriter(), payload);
    }

    private String normalizeMessage(AccessDeniedException ex) {
        String detail = ex.getMessage();
        if (detail == null || detail.isBlank()) {
            return "Access denied.";
        }
        return detail;
    }

    private String resolveTrackingId() {
        String value = MDC.get("tracking_id");
        return (value != null && !value.isBlank())
                ? value
                : MDC.get("trackingId");
    }
}