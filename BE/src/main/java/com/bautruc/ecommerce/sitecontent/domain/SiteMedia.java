package com.bautruc.ecommerce.sitecontent.domain;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "site_media")
public class SiteMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq")
    @SequenceGenerator(name = "global_seq", sequenceName = "app_global_id_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64, unique = true)
    private SiteMediaSlot slot;

    @Column(name = "object_key", length = 512, unique = true)
    private String objectKey;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SiteMedia() {
    }

    public Long getId() { return id; }
    public SiteMediaSlot getSlot() { return slot; }
    public String getObjectKey() { return objectKey; }
    public String getContentType() { return contentType; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void replace(String objectKey, String contentType, long fileSizeBytes, Instant now) {
        if (objectKey == null || objectKey.isBlank() || contentType == null || contentType.isBlank()
                || fileSizeBytes <= 0 || now == null) {
            throw new IllegalArgumentException("Invalid site media metadata");
        }
        this.objectKey = objectKey.trim();
        this.contentType = contentType.trim();
        this.fileSizeBytes = fileSizeBytes;
        this.updatedAt = now;
    }

    public void clear(Instant now) {
        if (now == null) throw new IllegalArgumentException("now is required");
        this.objectKey = null;
        this.contentType = null;
        this.fileSizeBytes = null;
        this.updatedAt = now;
    }
}
