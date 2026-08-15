package com.bautruc.ecommerce.catalog.api.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReorderProductImagesRequest(
        @NotNull @Size(max = 10) List<@NotNull Long> imageIds
) {
}
