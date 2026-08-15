package com.bautruc.ecommerce.notification.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.bautruc.ecommerce.common.response.PageResponse;
import com.bautruc.ecommerce.notification.api.response.NotificationResponse;
import com.bautruc.ecommerce.notification.domain.NotificationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationQueryRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public NotificationQueryRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc; this.mapper = mapper;
    }

    public PageResponse<NotificationResponse> findForAdmin(Long adminId, Boolean isRead, NotificationType type,
                                                           Integer requestedPage, Integer requestedSize, String sort) {
        int page = requestedPage == null ? PageResponse.DEFAULT_PAGE : Math.max(0, requestedPage);
        int size = requestedSize == null ? PageResponse.DEFAULT_SIZE
                : Math.min(Math.max(1, requestedSize), PageResponse.MAX_SIZE);
        StringBuilder where = new StringBuilder(" WHERE nr.admin_id=?");
        List<Object> arguments = new ArrayList<>();
        arguments.add(adminId);
        if (isRead != null) { where.append(" AND nr.is_read=?"); arguments.add(isRead); }
        if (type != null) { where.append(" AND n.type=?"); arguments.add(type.name()); }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM notification_recipients nr JOIN notifications n ON n.id=nr.notification_id" + where,
                Long.class, arguments.toArray());
        String direction = sort != null && sort.equalsIgnoreCase("createdAt,asc") ? "ASC" : "DESC";
        List<Object> pageArguments = new ArrayList<>(arguments);
        pageArguments.add(size); pageArguments.add((long) page * size);
        List<NotificationResponse> content = jdbc.query("""
                SELECT n.id,n.type,n.title,n.message,n.reference_type,n.reference_id,
                       n.metadata::text,nr.is_read,nr.read_at,n.created_at
                FROM notification_recipients nr
                JOIN notifications n ON n.id=nr.notification_id
                """ + where + " ORDER BY n.created_at " + direction + ", n.id " + direction + " LIMIT ? OFFSET ?",
                this::map, pageArguments.toArray());
        long count = total == null ? 0 : total;
        int totalPages = count == 0 ? 0 : (int) Math.ceil((double) count / size);
        return new PageResponse<>(content, page, size, count, totalPages, page == 0, page + 1 >= totalPages);
    }

    public NotificationResponse findForAdmin(Long adminId, Long notificationId) {
        List<NotificationResponse> rows = jdbc.query("""
                SELECT n.id,n.type,n.title,n.message,n.reference_type,n.reference_id,
                       n.metadata::text,nr.is_read,nr.read_at,n.created_at
                FROM notification_recipients nr
                JOIN notifications n ON n.id=nr.notification_id
                WHERE nr.admin_id=? AND n.id=?
                """, this::map, adminId, notificationId);
        return rows.stream().findFirst().orElse(null);
    }

    public int markRead(Long adminId, Long notificationId, java.time.Instant now) {
        return jdbc.update("""
                UPDATE notification_recipients SET is_read=true,read_at=?
                WHERE admin_id=? AND notification_id=? AND is_read=false
                """, java.sql.Timestamp.from(now), adminId, notificationId);
    }

    private NotificationResponse map(ResultSet rs, int row) throws SQLException {
        return new NotificationResponse(rs.getLong("id"), NotificationType.valueOf(rs.getString("type")),
                rs.getString("title"), rs.getString("message"), rs.getString("reference_type"),
                nullableLong(rs, "reference_id"), json(rs.getString("metadata")), rs.getBoolean("is_read"),
                rs.getTimestamp("read_at") == null ? null : rs.getTimestamp("read_at").toInstant(),
                rs.getTimestamp("created_at").toInstant());
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column); return rs.wasNull() ? null : value;
    }

    private JsonNode json(String value) throws SQLException {
        if (value == null) return null;
        try { return mapper.readTree(value); }
        catch (JsonProcessingException exception) { throw new SQLException("Invalid notification metadata", exception); }
    }
}
