package com.bautruc.ecommerce.common.security;

import java.io.IOException;
import java.time.OffsetDateTime;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiError;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityJsonResponseWriter {
    private final ObjectMapper objectMapper;
    private final BusinessClock businessClock;

    public SecurityJsonResponseWriter(ObjectMapper objectMapper, BusinessClock businessClock) {
        this.objectMapper = objectMapper;
        this.businessClock = businessClock;
    }

    public void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.failure(
                ApiError.of(code, message),
                responseTimestamp(),
                LogContext.currentCorrelationId()
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    public void writeSuccess(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.success(
                null,
                null,
                responseTimestamp(),
                LogContext.currentCorrelationId()
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private OffsetDateTime responseTimestamp() {
        return businessClock.businessNow().toOffsetDateTime();
    }
}
