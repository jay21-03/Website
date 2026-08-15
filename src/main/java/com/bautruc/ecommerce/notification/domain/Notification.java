package com.bautruc.ecommerce.notification.domain;

import java.time.Instant;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq")
    @SequenceGenerator(name = "global_seq", sequenceName = "app_global_id_seq", allocationSize = 1)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationType type;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String message;
    @Column(name = "reference_type") private String referenceType;
    @Column(name = "reference_id") private Long referenceId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String metadata;
    @Column(name = "dedup_key", nullable = false, unique = true) private String dedupKey;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected Notification() {}
    public Notification(NotificationType type, String title, String message, String referenceType,
                        Long referenceId, String metadata, String dedupKey, Instant createdAt) {
        this.type = type; this.title = title; this.message = message; this.referenceType = referenceType;
        this.referenceId = referenceId; this.metadata = metadata; this.dedupKey = dedupKey; this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getReferenceType() { return referenceType; }
    public Long getReferenceId() { return referenceId; }
    public String getMetadata() { return metadata; }
    public String getDedupKey() { return dedupKey; }
    public Instant getCreatedAt() { return createdAt; }
}
