package com.bautruc.ecommerce.notification.infrastructure;

import java.util.Optional;
import com.bautruc.ecommerce.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByDedupKey(String dedupKey);
}

