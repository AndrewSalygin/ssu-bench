package com.salygin.ssubench.security;

import com.salygin.ssubench.api.error.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JsonBadRequestEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            @NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            AuthenticationException ex
    ) throws IOException {

        int code = HttpServletResponse.SC_BAD_REQUEST;
        String msg = resolveMessage(ex);

        res.setStatus(code);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiErrorResponse error = new ApiErrorResponse(
                code,
                "Bad Request",
                msg,
                req.getRequestURI(),
                obtainTrackingId()
        );

        objectMapper.writeValue(res.getWriter(), error);
    }

    private String resolveMessage(AuthenticationException ex) {
        String detail = ex.getMessage();
        if (detail == null || detail.isBlank()) {
            return "Bad request.";
        }
        return detail;
    }

    private String obtainTrackingId() {
        String primary = MDC.get("tracking_id");
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return MDC.get("trackingId");
    }
}

