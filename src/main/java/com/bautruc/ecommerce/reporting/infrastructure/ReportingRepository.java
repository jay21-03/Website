package com.bautruc.ecommerce.reporting.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.bautruc.ecommerce.order.domain.OrderStatus;
import com.bautruc.ecommerce.payment.domain.PaymentStatus;
import com.bautruc.ecommerce.reporting.api.response.*;
import com.bautruc.ecommerce.reporting.domain.ReportGroupBy;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReportingRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public ReportingRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long totalOrders() { return number("SELECT COUNT(*) FROM orders", Map.of()); }

    public long totalRevenue() { return number("""
            SELECT COALESCE(SUM(o.total_amount),0) FROM orders o
            JOIN payments p ON p.order_id=o.id
            WHERE o.status='COMPLETED' AND p.status='PAID'
            """, Map.of()); }

    public long newOrders() { return number("""
            SELECT COUNT(*) FROM orders o JOIN payments p ON p.order_id=o.id
            WHERE o.status='NEW' AND p.status='PAID'
            """, Map.of()); }

    public long revenue(Instant start, Instant endExclusive) {
        return number("""
                SELECT COALESCE(SUM(o.total_amount),0) FROM orders o
                JOIN payments p ON p.order_id=o.id
                WHERE o.status='COMPLETED' AND p.status='PAID'
                  AND o.completed_at>=:start AND o.completed_at<:end
                """, Map.of("start", java.sql.Timestamp.from(start), "end", java.sql.Timestamp.from(endExclusive)));
    }

    public List<RevenuePointResponse> revenueChart(Instant start, Instant endExclusive, ReportGroupBy groupBy) {
        String bucket = groupBy.name().toLowerCase(java.util.Locale.ROOT);
        return jdbc.query("""
                SELECT date_trunc(:bucket,o.completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date period_start,
                       SUM(o.total_amount) revenue
                FROM orders o JOIN payments p ON p.order_id=o.id
                WHERE o.status='COMPLETED' AND p.status='PAID'
                  AND o.completed_at>=:start AND o.completed_at<:end
                GROUP BY period_start ORDER BY period_start
                """, Map.of("bucket", bucket, "start", java.sql.Timestamp.from(start), "end", java.sql.Timestamp.from(endExclusive)),
                (rs, row) -> new RevenuePointResponse(rs.getObject("period_start", LocalDate.class), rs.getLong("revenue")));
    }

    public List<RevenuePointResponse> allTimeRevenueChart() {
        return jdbc.query("""
                SELECT date_trunc('day',o.completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date period_start,
                       SUM(o.total_amount) revenue
                FROM orders o JOIN payments p ON p.order_id=o.id
                WHERE o.status='COMPLETED' AND p.status='PAID'
                GROUP BY period_start ORDER BY period_start
                """, Map.of(), (rs, row) -> new RevenuePointResponse(
                rs.getObject("period_start", LocalDate.class), rs.getLong("revenue")));
    }

    public List<BestSellingProductResponse> bestSelling(Instant start, Instant endExclusive, int limit) {
        Map<String,Object> params = new HashMap<>(); params.put("limit", limit);
        StringBuilder range = new StringBuilder();
        if (start != null) { range.append(" AND o.completed_at>=:start"); params.put("start", java.sql.Timestamp.from(start)); }
        if (endExclusive != null) { range.append(" AND o.completed_at<:end"); params.put("end", java.sql.Timestamp.from(endExclusive)); }
        return jdbc.query("""
                SELECT oi.product_id,
                       (array_agg(oi.product_name_vi ORDER BY o.created_at DESC))[1] product_name_vi,
                       (array_agg(oi.product_name_en ORDER BY o.created_at DESC))[1] product_name_en,
                       SUM(oi.quantity) sold_quantity
                FROM order_items oi
                JOIN orders o ON o.id=oi.order_id
                JOIN payments p ON p.order_id=o.id
                WHERE o.status='COMPLETED' AND p.status='PAID'
                """ + range + "\n" + """
                GROUP BY oi.product_id
                ORDER BY sold_quantity DESC,oi.product_id
                LIMIT :limit
                """, params, (rs, row) -> new BestSellingProductResponse(rs.getLong("product_id"),
                rs.getString("product_name_vi"), rs.getString("product_name_en"), rs.getLong("sold_quantity")));
    }

    public List<RecentOrderResponse> recentOrders(int limit) {
        return jdbc.query("""
                SELECT o.id,o.order_code,o.created_at,o.total_amount,o.status order_status,p.status payment_status
                FROM orders o JOIN payments p ON p.order_id=o.id
                ORDER BY o.created_at DESC,o.id DESC LIMIT :limit
                """, Map.of("limit", limit), this::recentOrder);
    }

    public List<LowStockProductResponse> lowStockProducts(int limit) {
        return jdbc.query("""
                SELECT p.id product_id,p.name_vi,p.name_en,
                       i.quantity-i.reserved_quantity available_quantity,i.low_stock_threshold
                FROM inventories i JOIN products p ON p.id=i.product_id
                WHERE p.deleted_at IS NULL
                  AND i.quantity-i.reserved_quantity>0
                  AND i.quantity-i.reserved_quantity<=i.low_stock_threshold
                ORDER BY available_quantity,p.id LIMIT :limit
                """, Map.of("limit", limit), (rs, row) -> new LowStockProductResponse(rs.getLong("product_id"),
                rs.getString("name_vi"), rs.getString("name_en"), rs.getLong("available_quantity"),
                rs.getLong("low_stock_threshold")));
    }

    private RecentOrderResponse recentOrder(ResultSet rs, int row) throws SQLException {
        return new RecentOrderResponse(rs.getLong("id"), rs.getString("order_code"),
                rs.getTimestamp("created_at").toInstant(), rs.getLong("total_amount"),
                OrderStatus.valueOf(rs.getString("order_status")), PaymentStatus.valueOf(rs.getString("payment_status")));
    }

    private long number(String sql, Map<String,?> params) {
        Long value = jdbc.queryForObject(sql, params, Long.class); return value == null ? 0 : value;
    }
}
