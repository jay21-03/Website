package com.bautruc.ecommerce.reporting.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.reporting.api.response.*;
import com.bautruc.ecommerce.reporting.domain.ReportGroupBy;
import com.bautruc.ecommerce.reporting.infrastructure.ReportingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingQueryService {
    private final ReportingRepository reports;
    private final BusinessClock clock;
    private final int dashboardSize;

    public ReportingQueryService(ReportingRepository reports, BusinessClock clock,
            @Value("${bautruc.reporting.dashboard-size:10}") int dashboardSize) {
        this.reports = reports; this.clock = clock; this.dashboardSize = Math.min(Math.max(1, dashboardSize), 100);
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        return new DashboardResponse(reports.totalOrders(), reports.totalRevenue(), reports.newOrders(),
                reports.lowStockProducts(dashboardSize), reports.allTimeRevenueChart(),
                reports.recentOrders(dashboardSize), reports.bestSelling(null, null, dashboardSize));
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse revenue(LocalDate fromDate, LocalDate toDate, ReportGroupBy groupBy) {
        requireRange(fromDate, toDate);
        if (groupBy == null) throw invalidRange("groupBy is required.");
        Instant start = clock.startOfDay(fromDate);
        Instant end = clock.startOfNextDay(toDate);
        return new RevenueReportResponse(fromDate, toDate, groupBy, reports.revenue(start, end),
                reports.revenueChart(start, end, groupBy));
    }

    @Transactional(readOnly = true)
    public List<BestSellingProductResponse> bestSelling(LocalDate fromDate, LocalDate toDate, Integer requestedLimit) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) throw invalidRange("fromDate must not be after toDate.");
        int limit = requestedLimit == null ? 10 : Math.min(Math.max(1, requestedLimit), 100);
        Instant start = fromDate == null ? null : clock.startOfDay(fromDate);
        Instant end = toDate == null ? null : clock.startOfNextDay(toDate);
        return reports.bestSelling(start, end, limit);
    }

    private void requireRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) throw invalidRange("fromDate and toDate are required.");
        if (fromDate.isAfter(toDate)) throw invalidRange("fromDate must not be after toDate.");
    }

    private BusinessException invalidRange(String message) {
        return new BusinessException(ReportingErrorCodes.REPORT_DATE_RANGE_INVALID, message);
    }
}
