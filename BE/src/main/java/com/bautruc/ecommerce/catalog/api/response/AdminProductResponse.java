package com.bautruc.ecommerce.catalog.api.response;

import java.time.Instant;
import java.util.List;

import com.bautruc.ecommerce.catalog.domain.ProductStatus;

public record AdminProductResponse(
        Long id,
        String nameVi,
        String nameEn,
        String descriptionVi,
        String descriptionEn,
        long basePrice,
        long sellingPrice,
        ProductStatus status,
        Long collectionId,
        String thumbnailUrl,
        List<ProductImageResponse> images,
        DiscountResponse discount,
        Instant createdAt,
        Instant updatedAt
) {
}
