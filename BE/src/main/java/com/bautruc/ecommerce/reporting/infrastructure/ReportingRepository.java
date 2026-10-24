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

    public Map<String, Object> businessMetrics(Instant start, Instant endExclusive) {
        return jdbc.queryForMap("""
                WITH order_created AS (
                    SELECT COUNT(*) orders_created,
                           COUNT(*) FILTER (WHERE status='NEW') orders_new,
                           COUNT(*) FILTER (WHERE status='CONFIRMED') orders_confirmed,
                           COUNT(*) FILTER (WHERE status='COMPLETED') orders_completed_created,
                           COUNT(*) FILTER (WHERE status='CANCELLED') orders_cancelled
                    FROM orders WHERE created_at>=:start AND created_at<:end
                ), completed AS (
                    SELECT COUNT(*) orders_completed,
                           COUNT(*) FILTER (WHERE p.status='PAID') completed_paid_orders,
                           COALESCE(SUM(o.total_amount) FILTER (WHERE p.status='PAID'),0)::text completed_paid_revenue
                    FROM orders o JOIN payments p ON p.order_id=o.id
                    WHERE o.status='COMPLETED' AND o.completed_at>=:start AND o.completed_at<:end
                ), payment_status AS (
                    SELECT COUNT(*) FILTER (WHERE status='PAID') payments_paid,
                           COUNT(*) FILTER (WHERE status='PENDING') payments_pending,
                           COUNT(*) FILTER (WHERE status='FAILED') payments_failed,
                           COUNT(*) FILTER (WHERE status='EXPIRED') payments_expired,
                           COUNT(*) FILTER (WHERE status='REFUNDED') payments_refunded
                    FROM payments WHERE created_at>=:start AND created_at<:end
                ), payment_values AS (
                    SELECT (SELECT COALESCE(SUM(amount),0)::text FROM payments WHERE status='PAID' AND paid_at>=:start AND paid_at<:end) paid_payment_amount,
                           (SELECT COALESCE(SUM(amount),0)::text FROM payments WHERE status='REFUNDED' AND refunded_at>=:start AND refunded_at<:end) refunded_payment_amount
                ), sold AS (
                    SELECT COALESCE(SUM(oi.quantity),0) sold_quantity
                    FROM order_items oi JOIN orders o ON o.id=oi.order_id JOIN payments p ON p.order_id=o.id
                    WHERE o.status='COMPLETED' AND p.status='PAID' AND o.completed_at>=:start AND o.completed_at<:end
                ), customer AS (
                    SELECT COUNT(*) FILTER (WHERE created_at>=:start AND created_at<:end) new_customer_accounts,
                           COUNT(*) customer_accounts FROM users WHERE role='USER'
                ), workshop AS (
                    SELECT COUNT(*) workshop_bookings,
                           COUNT(*) FILTER (WHERE status='NEW') workshop_new,
                           COUNT(*) FILTER (WHERE status='CONFIRMED') workshop_confirmed,
                           COUNT(*) FILTER (WHERE status='COMPLETED') workshop_completed,
                           COUNT(*) FILTER (WHERE status='CANCELLED') workshop_cancelled,
                           COALESCE(SUM(participants) FILTER (WHERE status<>'CANCELLED'),0) workshop_participants
                    FROM workshop_bookings WHERE created_at>=:start AND created_at<:end
                ), review AS (
                    SELECT COUNT(*) reviews_total,
                           COUNT(*) FILTER (WHERE NOT hidden) reviews_visible,
                           ROUND(AVG(rating) FILTER (WHERE NOT hidden),2) visible_average_rating
                    FROM product_reviews WHERE created_at>=:start AND created_at<:end
                ), notification AS (
                    SELECT COUNT(DISTINCT n.id) notification_count,
                           COUNT(nr.id) notification_recipient_count,
                           COUNT(nr.id) FILTER (WHERE nr.is_read) notification_read_count
                    FROM notifications n LEFT JOIN notification_recipients nr ON nr.notification_id=n.id
                    WHERE n.created_at>=:start AND n.created_at<:end
                ), operations AS (
                    SELECT (SELECT COUNT(*) FROM chat_conversations WHERE status='OPEN') open_chats,
                           (SELECT COUNT(*) FROM workshop_offerings WHERE status='ACTIVE') active_workshops,
                           (SELECT COUNT(*) FROM products WHERE status='ACTIVE' AND deleted_at IS NULL) active_products,
                           (SELECT COALESCE(SUM(GREATEST(i.quantity-i.reserved_quantity,0)),0) FROM inventories i JOIN products p ON p.id=i.product_id WHERE p.status='ACTIVE' AND p.deleted_at IS NULL) available_units,
                           (SELECT COUNT(*) FROM inventories i JOIN products p ON p.id=i.product_id WHERE p.status='ACTIVE' AND p.deleted_at IS NULL AND i.quantity-i.reserved_quantity>0 AND i.quantity-i.reserved_quantity<=i.low_stock_threshold) low_stock_products,
                           (SELECT COUNT(*) FROM inventories i JOIN products p ON p.id=i.product_id WHERE p.status='ACTIVE' AND p.deleted_at IS NULL AND i.quantity-i.reserved_quantity<=0) out_of_stock_products
                )
                SELECT * FROM order_created CROSS JOIN completed CROSS JOIN payment_status
                CROSS JOIN payment_values CROSS JOIN sold CROSS JOIN customer CROSS JOIN workshop
                CROSS JOIN review CROSS JOIN notification CROSS JOIN operations
                """, Map.of("start", java.sql.Timestamp.from(start), "end", java.sql.Timestamp.from(endExclusive)));
    }

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

    public List<Map<String, String>> businessRevenueTrend(Instant start, Instant endExclusive, ReportGroupBy groupBy) {
        String bucket = groupBy.name().toLowerCase(java.util.Locale.ROOT);
        return jdbc.query("""
                SELECT date_trunc(:bucket,o.completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date period_start,
                       SUM(o.total_amount)::text revenue
                FROM orders o JOIN payments p ON p.order_id=o.id
                WHERE o.status='COMPLETED' AND p.status='PAID'
                  AND o.completed_at>=:start AND o.completed_at<:end
                GROUP BY period_start ORDER BY period_start
                """, Map.of("bucket", bucket, "start", java.sql.Timestamp.from(start), "end", java.sql.Timestamp.from(endExclusive)),
                (rs, row) -> Map.of("periodStart", rs.getString("period_start"), "revenue", rs.getString("revenue")));
    }

    public List<Map<String, Object>> workshopInterest(Instant start, Instant endExclusive) {
        return jdbc.query("""
                SELECT COALESCE(w.title, 'Custom request') title,
                       COUNT(*) bookings,
                       COALESCE(SUM(b.participants) FILTER (WHERE b.status<>'CANCELLED'),0) participants
                FROM workshop_bookings b LEFT JOIN workshop_offerings w ON w.id=b.workshop_id
                WHERE b.created_at>=:start AND b.created_at<:end
                GROUP BY b.workshop_id,w.title ORDER BY bookings DESC,title LIMIT 10
                """, Map.of("start", java.sql.Timestamp.from(start), "end", java.sql.Timestamp.from(endExclusive)),
                (rs, row) -> Map.of("title", rs.getString("title"), "bookings", rs.getLong("bookings"),
                        "participants", rs.getLong("participants")));
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
