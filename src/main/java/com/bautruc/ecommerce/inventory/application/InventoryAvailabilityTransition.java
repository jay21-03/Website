package com.bautruc.ecommerce.inventory.application;

import com.bautruc.ecommerce.inventory.domain.InventoryAvailabilityStatus;

public record InventoryAvailabilityTransition(
        Long inventoryTransactionId,
        Long productId,
        InventoryAvailabilityStatus beforeStatus,
        InventoryAvailabilityStatus afterStatus,
        long availableQuantity,
        long lowStockThreshold
) {}
