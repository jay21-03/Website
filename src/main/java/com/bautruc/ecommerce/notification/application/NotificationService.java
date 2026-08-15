package com.bautruc.ecommerce.notification.application;

import java.time.Instant;
import com.bautruc.ecommerce.identity.application.UserQueryService;
import com.bautruc.ecommerce.notification.domain.*;
import com.bautruc.ecommerce.notification.infrastructure.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class NotificationService {
    private final NotificationInsertRepository inserts;
    private final NotificationRecipientJpaRepository recipients;
    private final UserQueryService users;
    private final ApplicationEventPublisher events;
    public NotificationService(NotificationInsertRepository i, NotificationRecipientJpaRepository r,
                               UserQueryService u, ApplicationEventPublisher e) {
        inserts = i; recipients = r; users = u; events = e;
    }
    @Transactional
    public void create(NotificationType type, String title, String message, String referenceType,
                       Long referenceId, String metadata, String dedupKey, Instant now) {
        inserts.insert(type, title, message, referenceType, referenceId, metadata, dedupKey, now).ifPresent(id -> {
            java.util.List<Long> adminIds = users.activeAdminIds();
            recipients.saveAll(adminIds.stream().map(adminId -> new NotificationRecipient(id, adminId)).toList());
            events.publishEvent(new NotificationCreatedEvent(id, type, now, adminIds));
        });
    }
}
