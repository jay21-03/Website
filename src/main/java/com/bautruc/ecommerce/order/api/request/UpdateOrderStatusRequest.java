package com.bautruc.ecommerce.order.api.request;

import com.bautruc.ecommerce.order.domain.OrderStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
    @AssertTrue(message = "status must be CONFIRMED or COMPLETED")
    public boolean isSupportedTransition() {
        return status == null || status == OrderStatus.CONFIRMED || status == OrderStatus.COMPLETED;
    }
}
