package com.bautruc.ecommerce.notification.application;

import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.inventory.application.InventoryAvailabilityTransition;
import com.bautruc.ecommerce.inventory.domain.InventoryAvailabilityStatus;
import com.bautruc.ecommerce.notification.domain.NotificationType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryAvailabilityNotificationListener {
    private final NotificationService notifications;
    private final BusinessClock clock;

    public InventoryAvailabilityNotificationListener(NotificationService notifications, BusinessClock clock) {
        this.notifications = notifications; this.clock = clock;
    }

    @EventListener
    public void onTransition(InventoryAvailabilityTransition event) {
        if (event.afterStatus() != InventoryAvailabilityStatus.LOW_STOCK
                && event.afterStatus() != InventoryAvailabilityStatus.OUT_OF_STOCK) return;
        NotificationType type = event.afterStatus() == InventoryAvailabilityStatus.OUT_OF_STOCK
                ? NotificationType.OUT_OF_STOCK : NotificationType.LOW_STOCK;
        String title = type == NotificationType.OUT_OF_STOCK ? "Product out of stock" : "Product low in stock";
        String message = "Product " + event.productId() + " has " + event.availableQuantity() + " available item(s).";
        String metadata = "{\"availableQuantity\":" + event.availableQuantity()
                + ",\"lowStockThreshold\":" + event.lowStockThreshold() + "}";
        notifications.create(type, title, message, "PRODUCT", event.productId(), metadata,
                type + ":INV_TX:" + event.inventoryTransactionId(), clock.now());
    }
}
