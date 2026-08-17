package com.bautruc.ecommerce.catalog.api.request;

import com.bautruc.ecommerce.catalog.domain.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(@NotNull ProductStatus status) {
}
