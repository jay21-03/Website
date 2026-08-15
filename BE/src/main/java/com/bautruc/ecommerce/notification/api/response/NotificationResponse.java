package com.bautruc.ecommerce.notification.api.response;

import java.time.Instant;
import com.bautruc.ecommerce.notification.domain.NotificationType;
import com.fasterxml.jackson.databind.JsonNode;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String referenceType,
        Long referenceId,
        JsonNode metadata,
        boolean isRead,
        Instant readAt,
        Instant createdAt
) {}
