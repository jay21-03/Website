package com.bautruc.ecommerce.common.logging;

import org.slf4j.MDC;

public final class LogContext {
    public static final String CORRELATION_ID = "correlationId";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private LogContext() {
    }

    public static String currentCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }
}
