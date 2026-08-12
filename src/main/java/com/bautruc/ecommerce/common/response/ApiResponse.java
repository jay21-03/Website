package com.bautruc.ecommerce.common.response;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        ApiError error,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime timestamp,
        String correlationId
) {
    public static <T> ApiResponse<T> success(
            T data,
            String message,
            OffsetDateTime timestamp,
            String correlationId
    ) {
        return new ApiResponse<>(true, data, message, null, timestamp, correlationId);
    }

    public static ApiResponse<Void> ok(
            String message,
            OffsetDateTime timestamp,
            String correlationId
    ) {
        return new ApiResponse<>(true, null, message, null, timestamp, correlationId);
    }

    public static <T> ApiResponse<T> failure(
            ApiError error,
            OffsetDateTime timestamp,
            String correlationId
    ) {
        return new ApiResponse<>(false, null, null, error, timestamp, correlationId);
    }
}
