package com.bautruc.ecommerce.catalog.api.request;

import com.bautruc.ecommerce.catalog.domain.CollectionStatus;
import jakarta.validation.constraints.NotNull;

public record CollectionStatusRequest(@NotNull CollectionStatus status) {
}
