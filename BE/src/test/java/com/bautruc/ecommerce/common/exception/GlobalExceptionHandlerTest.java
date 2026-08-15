package com.bautruc.ecommerce.common.exception;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.bautruc.ecommerce.common.logging.CorrelationIdFilter;
import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.time.SystemBusinessClock;

class GlobalExceptionHandlerTest {
    private static final String BUSINESS_TIMESTAMP_PATTERN = "^2026-08-12T10:30:00(\\.0+)?\\+07:00$";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        SystemBusinessClock businessClock = new SystemBusinessClock(Clock.fixed(
                Instant.parse("2026-08-12T03:30:00Z"),
                ZoneOffset.UTC
        ));
        this.mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(businessClock))
                .setValidator(validator)
                .addFilters(new CorrelationIdFilter())
                .build();
    }

    @Test
    void mapsBusinessExceptionToApiErrorEnvelope() throws Exception {
        mockMvc.perform(get("/test/business-error").header(LogContext.CORRELATION_ID_HEADER, "corr-123"))
                .andExpect(status().isConflict())
                .andExpect(header().string(LogContext.CORRELATION_ID_HEADER, "corr-123"))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("TEST_CONFLICT")))
                .andExpect(jsonPath("$.error.message", is("Conflict for test")))
                .andExpect(jsonPath("$.timestamp", matchesPattern(BUSINESS_TIMESTAMP_PATTERN)))
                .andExpect(jsonPath("$.correlationId", is("corr-123")))
                .andExpect(jsonPath("$.error.correlationId").doesNotExist())
                .andExpect(jsonPath("$.error.timestamp").doesNotExist());
    }

    @Test
    void mapsValidationFailureToApiErrorEnvelope() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.message", is("Validation failed.")))
                .andExpect(jsonPath("$.error.fieldErrors[0].field", is("name")))
                .andExpect(jsonPath("$.timestamp", matchesPattern(BUSINESS_TIMESTAMP_PATTERN)))
                .andExpect(jsonPath("$.correlationId", matchesPattern("^[0-9a-fA-F-]{36}$")));
    }

    @Test
    void mapsResourceNotFoundTo404() throws Exception {
        mockMvc.perform(get("/test/not-found").header(LogContext.CORRELATION_ID_HEADER, "corr-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.correlationId", is("corr-not-found")));
    }

    @Test
    void mapsConflictTo409() throws Exception {
        mockMvc.perform(get("/test/conflict").header(LogContext.CORRELATION_ID_HEADER, "corr-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("CONFLICT")))
                .andExpect(jsonPath("$.correlationId", is("corr-conflict")));
    }

    @Test
    void mapsMalformedJsonTo400ValidationFailed() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.message", is("Malformed JSON request.")))
                .andExpect(jsonPath("$.timestamp", matchesPattern(BUSINESS_TIMESTAMP_PATTERN)))
                .andExpect(jsonPath("$.correlationId", matchesPattern("^[0-9a-fA-F-]{36}$")));
    }

    @Test
    void mapsUnexpectedExceptionTo500InternalError() throws Exception {
        mockMvc.perform(get("/test/unexpected").header(LogContext.CORRELATION_ID_HEADER, "corr-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INTERNAL_ERROR")))
                .andExpect(jsonPath("$.error.message", is("Unexpected server error")))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))))
                .andExpect(jsonPath("$.correlationId", is("corr-500")));
    }

    @Validated
    @RestController
    public static class TestController {
        @GetMapping("/test/business-error")
        void businessError() {
            throw new BusinessException("TEST_CONFLICT", "Conflict for test", HttpStatus.CONFLICT);
        }

        @GetMapping("/test/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Missing test resource");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("Conflicting test state");
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("secret stack detail must not be exposed");
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody ValidationRequest request) {
        }
    }

    record ValidationRequest(@NotBlank String name) {
    }
}
