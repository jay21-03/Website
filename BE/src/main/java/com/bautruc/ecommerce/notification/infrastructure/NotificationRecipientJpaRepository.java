package com.bautruc.ecommerce.notification.infrastructure;

import com.bautruc.ecommerce.notification.domain.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecipientJpaRepository extends JpaRepository<NotificationRecipient, Long> {}

