package com.bautruc.ecommerce.notification.application;

import java.time.Instant;
import java.util.List;
import com.bautruc.ecommerce.notification.domain.NotificationType;

public record NotificationCreatedEvent(Long notificationId, NotificationType type, Instant createdAt,
                                       List<Long> recipientAdminIds) {
    public NotificationCreatedEvent { recipientAdminIds = List.copyOf(recipientAdminIds); }
}
