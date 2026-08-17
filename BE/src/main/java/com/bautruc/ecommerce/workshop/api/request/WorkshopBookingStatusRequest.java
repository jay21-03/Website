package com.bautruc.ecommerce.workshop.api.request;

import com.bautruc.ecommerce.workshop.domain.WorkshopBookingStatus;
import jakarta.validation.constraints.NotNull;

public record WorkshopBookingStatusRequest(@NotNull WorkshopBookingStatus status) {
}
