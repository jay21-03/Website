package com.bautruc.ecommerce.reporting.api.response;

import java.util.List;

public record DashboardResponse(long totalOrders, long totalRevenue, long newOrders,
        List<LowStockProductResponse> lowStockProducts, List<RevenuePointResponse> revenueChart,
        List<RecentOrderResponse> recentOrders, List<BestSellingProductResponse> bestSellingProducts) {}
