package com.bautruc.ecommerce.catalog.application;

public record HomepageProductView(
        Long id,
        String slug,
        String nameVi,
        String nameEn,
        String descriptionVi,
        String descriptionEn,
        long basePrice,
        long sellingPrice,
        String thumbnailUrl
) {
}
