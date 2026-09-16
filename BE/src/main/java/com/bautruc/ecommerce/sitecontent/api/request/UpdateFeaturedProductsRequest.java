package com.bautruc.ecommerce.sitecontent.api.request;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateFeaturedProductsRequest(
        @NotNull
        @Size(min = 3, max = 3)
        List<@NotNull @Positive Long> productIds
) {
}
