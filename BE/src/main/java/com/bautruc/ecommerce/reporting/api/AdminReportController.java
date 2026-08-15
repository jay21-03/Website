package com.bautruc.ecommerce.reporting.api;

import java.time.LocalDate;
import java.util.List;
import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.reporting.api.response.BestSellingProductResponse;
import com.bautruc.ecommerce.reporting.api.response.RevenueReportResponse;
import com.bautruc.ecommerce.reporting.application.ReportingQueryService;
import com.bautruc.ecommerce.reporting.domain.ReportGroupBy;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {
    private final ReportingQueryService reports;
    private final BusinessClock clock;
    public AdminReportController(ReportingQueryService reports, BusinessClock clock) {
        this.reports = reports; this.clock = clock;
    }

    @GetMapping("/revenue")
    public ApiResponse<RevenueReportResponse> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam ReportGroupBy groupBy) {
        return ok(reports.revenue(fromDate, toDate, groupBy));
    }

    @GetMapping("/best-selling")
    public ApiResponse<List<BestSellingProductResponse>> bestSelling(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer limit) {
        return ok(reports.bestSelling(fromDate, toDate, limit));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, null, clock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
