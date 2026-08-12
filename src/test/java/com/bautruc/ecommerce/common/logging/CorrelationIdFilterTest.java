package com.bautruc.ecommerce.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesIncomingCorrelationIdInHeaderAndMdcThenCleansUp() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader(LogContext.CORRELATION_ID_HEADER, "corr-incoming");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringRequest = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                mdcDuringRequest.set(LogContext.currentCorrelationId())
        );

        assertThat(response.getHeader(LogContext.CORRELATION_ID_HEADER)).isEqualTo("corr-incoming");
        assertThat(mdcDuringRequest).hasValue("corr-incoming");
        assertThat(MDC.get(LogContext.CORRELATION_ID)).isNull();
    }

    @Test
    void generatesUuidCorrelationIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringRequest = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                mdcDuringRequest.set(LogContext.currentCorrelationId())
        );

        String generatedCorrelationId = response.getHeader(LogContext.CORRELATION_ID_HEADER);
        assertThat(generatedCorrelationId).matches("^[0-9a-fA-F-]{36}$");
        assertThat(mdcDuringRequest).hasValue(generatedCorrelationId);
        assertThat(MDC.get(LogContext.CORRELATION_ID)).isNull();
    }
}
