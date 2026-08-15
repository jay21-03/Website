package com.bautruc.ecommerce.reporting.api;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.reporting.api.response.DashboardResponse;
import com.bautruc.ecommerce.reporting.application.ReportingQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminDashboardController {
    private final ReportingQueryService reports;
    private final BusinessClock clock;
    public AdminDashboardController(ReportingQueryService reports, BusinessClock clock) {
        this.reports = reports; this.clock = clock;
    }

    @GetMapping("/api/v1/admin/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.success(reports.dashboard(), null, clock.businessNow().toOffsetDateTime(),
                LogContext.currentCorrelationId());
    }
}
