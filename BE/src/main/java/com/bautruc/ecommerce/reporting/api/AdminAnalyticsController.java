package com.bautruc.ecommerce.reporting.api;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.reporting.application.Ga4AnalyticsService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAnalyticsController {
    private final Ga4AnalyticsService analytics;
    private final BusinessClock clock;

    public AdminAnalyticsController(Ga4AnalyticsService analytics, BusinessClock clock) {
        this.analytics = analytics;
        this.clock = clock;
    }

    @GetMapping("/api/v1/admin/analytics")
    public ApiResponse<Map<String, Object>> dashboard() {
        return response(analytics.dashboard());
    }

    @GetMapping("/api/v1/admin/analytics/realtime")
    public ApiResponse<Map<String, Object>> realtime() {
        return response(analytics.realtime());
    }

    @GetMapping("/api/v1/admin/analytics/historical")
    public ApiResponse<Map<String, Object>> historical(@RequestParam(defaultValue = "30") int days) {
        return response(analytics.historical(days));
    }

    private ApiResponse<Map<String, Object>> response(Map<String, Object> data) {
        return ApiResponse.success(data, null, clock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
