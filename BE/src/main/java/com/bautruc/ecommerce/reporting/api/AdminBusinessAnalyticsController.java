package com.bautruc.ecommerce.reporting.api;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.reporting.application.ReportingQueryService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminBusinessAnalyticsController {
    private final ReportingQueryService reports;
    private final BusinessClock clock;

    public AdminBusinessAnalyticsController(ReportingQueryService reports, BusinessClock clock) {
        this.reports = reports;
        this.clock = clock;
    }

    @GetMapping("/api/v1/admin/analytics/business")
    public ApiResponse<Map<String, Object>> business(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(reports.businessAnalytics(days), null, clock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
