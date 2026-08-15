package com.bautruc.ecommerce.notification.application;

import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.common.response.PageResponse;
import com.bautruc.ecommerce.common.security.CurrentUserProvider;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.notification.api.response.NotificationResponse;
import com.bautruc.ecommerce.notification.domain.NotificationType;
import com.bautruc.ecommerce.notification.infrastructure.NotificationQueryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminNotificationService {
    private final CurrentUserProvider currentUser;
    private final NotificationQueryRepository notifications;
    private final BusinessClock clock;

    public AdminNotificationService(CurrentUserProvider currentUser, NotificationQueryRepository notifications,
                                    BusinessClock clock) {
        this.currentUser = currentUser; this.notifications = notifications; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Boolean isRead, NotificationType type, Integer page,
                                                   Integer size, String sort) {
        return notifications.findForAdmin(adminId(), isRead, type, page, size, sort);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        Long adminId = adminId();
        notifications.markRead(adminId, notificationId, clock.now());
        NotificationResponse result = notifications.findForAdmin(adminId, notificationId);
        if (result == null) throw new ResourceNotFoundException(NotificationErrorCodes.NOTIFICATION_NOT_FOUND,
                "Notification not found for current admin.");
        return result;
    }

    public Long currentAdminId() { return adminId(); }

    private Long adminId() {
        return currentUser.currentUser().orElseThrow(() -> new AccessDeniedException("Authentication required")).userId();
    }
}
