package com.bautruc.ecommerce.notification.application;

import java.util.Map;
import com.bautruc.ecommerce.notification.infrastructure.SseEmitterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationAfterCommitListener {
    private final SseEmitterRegistry emitters;
    public NotificationAfterCommitListener(SseEmitterRegistry emitters) { this.emitters = emitters; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(NotificationCreatedEvent event) {
        Map<String, Object> data = Map.of("notificationId", event.notificationId(), "type", event.type().name(),
                "createdAt", event.createdAt().toString());
        event.recipientAdminIds().forEach(adminId -> emitters.send(adminId, "notification", data));
    }
}
