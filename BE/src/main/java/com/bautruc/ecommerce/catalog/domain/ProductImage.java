package com.bautruc.ecommerce.catalog.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images")
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq")
    @SequenceGenerator(name = "global_seq", sequenceName = "app_global_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "object_key", nullable = false, length = 512, unique = true)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(name = "is_thumbnail", nullable = false)
    private boolean thumbnail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProductImage() {
    }

    public ProductImage(Long productId, String objectKey, String contentType, long fileSizeBytes,
                        short sortOrder, boolean thumbnail, Instant createdAt) {
        if (productId == null || objectKey == null || objectKey.isBlank() || contentType == null
                || contentType.isBlank() || fileSizeBytes <= 0 || sortOrder < 0 || createdAt == null) {
            throw new IllegalArgumentException("Invalid product image metadata");
        }
        this.productId = productId;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.sortOrder = sortOrder;
        this.thumbnail = thumbnail;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getObjectKey() { return objectKey; }
    public String getContentType() { return contentType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public short getSortOrder() { return sortOrder; }
    public boolean isThumbnail() { return thumbnail; }
    public Instant getCreatedAt() { return createdAt; }
    public void setSortOrder(short sortOrder) { if (sortOrder < 0) throw new IllegalArgumentException("sortOrder must not be negative"); this.sortOrder = sortOrder; }
    public void setThumbnail(boolean thumbnail) { this.thumbnail = thumbnail; }
}
