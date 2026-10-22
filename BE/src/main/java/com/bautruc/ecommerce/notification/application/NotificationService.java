package com.bautruc.ecommerce.notification.application;

import java.time.Instant;
import com.bautruc.ecommerce.identity.application.UserQueryService;
import com.bautruc.ecommerce.notification.domain.*;
import com.bautruc.ecommerce.notification.infrastructure.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class NotificationService {
    private final NotificationInsertRepository inserts;
    private final NotificationRecipientJpaRepository recipients;
    private final UserQueryService users;
    private final ApplicationEventPublisher events;
    private final JdbcTemplate jdbc;
    public NotificationService(NotificationInsertRepository i, NotificationRecipientJpaRepository r,
                               UserQueryService u, ApplicationEventPublisher e, JdbcTemplate j) {
        inserts = i; recipients = r; users = u; events = e; jdbc = j;
    }
    @Transactional
    public void create(NotificationType type, String title, String message, String referenceType,
                       Long referenceId, String metadata, String dedupKey, Instant now) {
        create(type, title, message, referenceType, referenceId, metadata, dedupKey, now, null);
    }

    @Transactional
    public void create(NotificationType type, String title, String message, String referenceType,
                       Long referenceId, String metadata, String dedupKey, Instant now, Long customerId) {
        inserts.insert(type, title, message, referenceType, referenceId, metadata, dedupKey, now).ifPresent(id -> {
            java.util.List<Long> adminIds = users.activeAdminIds();
            recipients.saveAll(adminIds.stream().map(adminId -> new NotificationRecipient(id, adminId)).toList());
            if (customerId != null) jdbc.update("INSERT INTO customer_notification_recipients(notification_id,user_id) VALUES (?,?)", id, customerId);
            events.publishEvent(new NotificationCreatedEvent(id, type, now, adminIds));
        });
    }
}
