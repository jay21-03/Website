package com.bautruc.ecommerce.notification.application;

import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.common.response.PageResponse;
import com.bautruc.ecommerce.common.security.CurrentUserProvider;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.notification.api.response.NotificationResponse;
import com.bautruc.ecommerce.notification.infrastructure.NotificationQueryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerNotificationService {
    private final CurrentUserProvider currentUser;
    private final NotificationQueryRepository notifications;
    private final BusinessClock clock;

    public CustomerNotificationService(CurrentUserProvider currentUser, NotificationQueryRepository notifications, BusinessClock clock) {
        this.currentUser = currentUser; this.notifications = notifications; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Boolean isRead, Integer page, Integer size) {
        return notifications.findForCustomer(userId(), isRead, page, size);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        Long userId = userId();
        notifications.markCustomerRead(userId, notificationId, clock.now());
        NotificationResponse result = notifications.findForCustomer(userId, notificationId);
        if (result == null) throw new ResourceNotFoundException(NotificationErrorCodes.NOTIFICATION_NOT_FOUND,
                "Notification not found for current customer.");
        return result;
    }

    private Long userId() {
        return currentUser.currentUser().orElseThrow(() -> new AccessDeniedException("Authentication required")).userId();
    }
}
