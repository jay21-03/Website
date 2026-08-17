package com.bautruc.ecommerce.catalog.api.request;

import jakarta.validation.constraints.NotNull;

public record DiscountActiveRequest(@NotNull Boolean isActive) {
}
