package com.bautruc.ecommerce.reporting.api.response;

import java.time.LocalDate;
import java.util.List;
import com.bautruc.ecommerce.reporting.domain.ReportGroupBy;

public record RevenueReportResponse(LocalDate fromDate, LocalDate toDate, ReportGroupBy groupBy,
                                    long totalRevenue, List<RevenuePointResponse> points) {}
