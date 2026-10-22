package com.bautruc.ecommerce.notification.api;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.response.PageResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.notification.api.response.NotificationResponse;
import com.bautruc.ecommerce.notification.application.CustomerNotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/notifications")
public class CustomerNotificationController {
    private final CustomerNotificationService notifications;
    private final BusinessClock clock;

    public CustomerNotificationController(CustomerNotificationService notifications, BusinessClock clock) {
        this.notifications = notifications; this.clock = clock;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(@RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ok(notifications.list(isRead, page, size));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable Long id) {
        return ok(notifications.markRead(id));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, null, clock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
