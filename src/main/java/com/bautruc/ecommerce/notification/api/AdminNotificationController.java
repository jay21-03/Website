package com.bautruc.ecommerce.notification.api;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.response.PageResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.notification.api.response.NotificationResponse;
import com.bautruc.ecommerce.notification.application.AdminNotificationService;
import com.bautruc.ecommerce.notification.domain.NotificationType;
import com.bautruc.ecommerce.notification.infrastructure.SseEmitterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class AdminNotificationController {
    private final AdminNotificationService notifications;
    private final SseEmitterRegistry emitters;
    private final BusinessClock clock;

    public AdminNotificationController(AdminNotificationService notifications, SseEmitterRegistry emitters,
                                       BusinessClock clock) {
        this.notifications = notifications; this.emitters = emitters; this.clock = clock;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(@RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationType type, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) String sort) {
        return ok(notifications.list(isRead, type, page, size, sort));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable Long id) { return ok(notifications.markRead(id)); }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() { return emitters.connect(notifications.currentAdminId(), clock.now()); }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, null, clock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
