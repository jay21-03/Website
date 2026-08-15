package com.bautruc.ecommerce.notification.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_recipients")
public class NotificationRecipient {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq")
    @SequenceGenerator(name = "global_seq", sequenceName = "app_global_id_seq", allocationSize = 1)
    private Long id;
    @Column(name = "notification_id", nullable = false) private Long notificationId;
    @Column(name = "admin_id", nullable = false) private Long adminId;
    @Column(name = "is_read", nullable = false) private boolean read;
    @Column(name = "read_at") private java.time.Instant readAt;
    protected NotificationRecipient() {}
    public NotificationRecipient(Long notificationId, Long adminId) { this.notificationId = notificationId; this.adminId = adminId; }
    public Long getId() { return id; }
    public Long getNotificationId() { return notificationId; }
    public Long getAdminId() { return adminId; }
    public boolean isRead() { return read; }
    public java.time.Instant getReadAt() { return readAt; }
    public void markRead(java.time.Instant now) { if (!read) { read = true; readAt = now; } }
}
