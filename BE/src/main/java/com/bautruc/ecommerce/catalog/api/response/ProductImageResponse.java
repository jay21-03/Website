package com.bautruc.ecommerce.catalog.api.response;

import java.time.Instant;

import com.bautruc.ecommerce.catalog.domain.ProductImage;

public record ProductImageResponse(Long id, String url, String contentType, long fileSizeBytes,
                                   short sortOrder, boolean thumbnail, Instant createdAt) {
    public static ProductImageResponse from(ProductImage image, String publicUrl) {
        return new ProductImageResponse(image.getId(), publicUrl, image.getContentType(), image.getFileSizeBytes(),
                image.getSortOrder(), image.isThumbnail(), image.getCreatedAt());
    }
}
