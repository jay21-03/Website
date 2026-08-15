package com.bautruc.ecommerce.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void createsSuccessEnvelopeWithTopLevelTimestampAndCorrelationId() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-12T10:30:00+07:00");

        ApiResponse<Map<String, String>> response = ApiResponse.success(
                Map.of("id", "123"),
                null,
                timestamp,
                "corr-success"
        );

        assertThat(response.success()).isTrue();
        assertThat(response.data()).containsEntry("id", "123");
        assertThat(response.message()).isNull();
        assertThat(response.timestamp()).isEqualTo(timestamp);
        assertThat(response.correlationId()).isEqualTo("corr-success");
        assertThat(response.error()).isNull();
    }

    @Test
    void createsErrorEnvelopeWithLogicalErrorOnly() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-12T10:30:00+07:00");
        ApiError error = ApiError.of("CONFLICT", "Conflicting state");

        ApiResponse<Void> response = ApiResponse.failure(error, timestamp, "corr-error");

        assertThat(response.success()).isFalse();
        assertThat(response.error()).isEqualTo(error);
        assertThat(response.timestamp()).isEqualTo(timestamp);
        assertThat(response.correlationId()).isEqualTo("corr-error");
        assertThat(response.error().fieldErrors()).isEmpty();
    }
}
