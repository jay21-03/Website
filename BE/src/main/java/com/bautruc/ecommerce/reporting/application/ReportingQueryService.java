package com.bautruc.ecommerce.reporting.application;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
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
    public Map<String, Object> businessAnalytics(int days) {
        if (days != 7 && days != 30 && days != 90) throw invalidRange("days must be 7, 30 or 90.");
        LocalDate today = clock.businessNow().toLocalDate();
        LocalDate from = today.minusDays(days - 1L);
        Instant start = clock.startOfDay(from);
        Instant end = clock.startOfNextDay(today);
        Map<String, Object> metrics = new LinkedHashMap<>(reports.businessMetrics(start, end));
        long paidOrders = ((Number) metrics.get("completed_paid_orders")).longValue();
        metrics.put("average_order_value", paidOrders == 0 ? null :
                new BigDecimal((String) metrics.get("completed_paid_revenue"))
                        .divide(BigDecimal.valueOf(paidOrders), 0, RoundingMode.HALF_UP).toPlainString());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "POSTGRESQL");
        result.put("status", "READY");
        result.put("timezone", clock.businessZone().getId());
        result.put("period", Map.of("fromDate", from.toString(), "toDate", today.toString(), "includesToday", true));
        result.put("fetchedAt", clock.now().toString());
        result.put("dateBasis", Map.of("ordersCreated", "orders.created_at", "ordersCompletedAndRevenue", "orders.completed_at",
                "paymentsPaid", "payments.paid_at", "paymentsRefunded", "payments.refunded_at",
                "workshops", "workshop_bookings.created_at", "reviews", "product_reviews.created_at",
                "notifications", "notifications.created_at"));
        result.put("limitations", List.of("Status counts use current status, not a historical snapshot.",
                "Completed paid order value is not net revenue; refunds and fees are separate.",
                "Workshop bookings are not paid workshop revenue; average rating is unavailable without visible reviews."));
        result.put("metrics", metrics);
        result.put("revenueTrend", reports.businessRevenueTrend(start, end, days == 90 ? ReportGroupBy.WEEK : ReportGroupBy.DAY));
        result.put("bestSelling", reports.bestSelling(start, end, 10));
        result.put("workshopInterest", reports.workshopInterest(start, end));
        return result;
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
