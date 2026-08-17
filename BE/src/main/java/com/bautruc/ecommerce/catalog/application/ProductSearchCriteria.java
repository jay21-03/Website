package com.bautruc.ecommerce.catalog.application;

public record ProductSearchCriteria(
        String keyword,
        Long collectionId,
        Long minPrice,
        Long maxPrice,
        int page,
        int size,
        ProductSort sort
) {
}
