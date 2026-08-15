package com.bautruc.ecommerce.notification.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.bautruc.ecommerce.notification.domain.NotificationType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationInsertRepository {
    private final JdbcTemplate jdbc;

    public NotificationInsertRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Long> insert(NotificationType type, String title, String message, String referenceType,
                                 Long referenceId, String metadata, String dedupKey, Instant createdAt) {
        List<Long> ids = jdbc.query("""
                INSERT INTO notifications(type,title,message,reference_type,reference_id,metadata,dedup_key,created_at)
                VALUES (?,?,?,?,?,CAST(? AS jsonb),?,?)
                ON CONFLICT (dedup_key) DO NOTHING
                RETURNING id
                """, (rs, row) -> rs.getLong(1), type.name(), title, message, referenceType,
                referenceId, metadata, dedupKey, java.sql.Timestamp.from(createdAt));
        return ids.stream().findFirst();
    }
}
