package com.salygin.ssubench.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> onIllegalArgument(IllegalArgumentException ex,
                                                              HttpServletRequest req) {
        return buildResponse(ex.getMessage(), req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> onConstraintViolation(ConstraintViolationException ex,
                                                                  HttpServletRequest req) {
        return buildResponse(ex.getMessage(), req);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(String msg,
                                                           HttpServletRequest req) {

        int code = HttpStatus.BAD_REQUEST.value();
        String reason = HttpStatus.BAD_REQUEST.getReasonPhrase();

        ApiErrorResponse error = new ApiErrorResponse(
                code,
                reason,
                msg,
                req.getRequestURI(),
                resolveRequestId()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    private String resolveRequestId() {
        String id = MDC.get("tracking_id");
        return (id != null && !id.isBlank())
                ? id
                : MDC.get("trackingId");
    }
}
