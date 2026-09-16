package com.bautruc.ecommerce.sitecontent.application;

import java.io.IOException;
import java.util.UUID;

import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.storage.ObjectStorageException;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SiteMediaService {
    private static final Logger log = LoggerFactory.getLogger(SiteMediaService.class);
    private static final int DELETE_ATTEMPTS = 3;

    private final SiteMediaMetadataService metadata;
    private final ObjectStoragePort storage;
    private final SiteMediaImageValidator signatureValidator;
    private final long maxBytes;

    public SiteMediaService(
            SiteMediaMetadataService metadata,
            ObjectStoragePort storage,
            SiteMediaImageValidator signatureValidator,
            ApplicationProperties properties
    ) {
        this.metadata = metadata;
        this.storage = storage;
        this.signatureValidator = signatureValidator;
        this.maxBytes = properties.image() == null || properties.image().maxBytes() <= 0
                ? 5L * 1024 * 1024
                : properties.image().maxBytes();
    }

    public SiteMediaView upload(SiteMediaSlot slot, MultipartFile file) {
        if (slot == null) throw slotNotFound();
        if (file == null || file.isEmpty()) throw emptyImage();
        if (file.getSize() > maxBytes) throw tooLarge();

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(
                    SiteContentErrorCodes.HOMEPAGE_MEDIA_SIGNATURE_INVALID,
                    "Could not read uploaded homepage image."
            );
        }
        if (bytes.length > maxBytes) throw tooLarge();

        ValidatedSiteMediaImage validated = signatureValidator.validate(bytes, file.getContentType());
        String objectKey = slot.objectKeyPrefix() + UUID.randomUUID() + "." + validated.extension();

        try {
            storage.put(objectKey, validated.content(), validated.contentType());
        } catch (ObjectStorageException exception) {
            throw new BusinessException(
                    SiteContentErrorCodes.HOMEPAGE_MEDIA_STORAGE_UPLOAD_FAILED,
                    "Homepage image storage upload failed.",
                    HttpStatus.BAD_GATEWAY
            );
        }

        SiteMediaMetadataChange change;
        try {
            change = metadata.replace(slot, objectKey, validated.contentType(), validated.content().length);
        } catch (RuntimeException exception) {
            deleteWithRetry(objectKey, "homepage-upload-compensation");
            throw exception;
        }

        if (change.previousObjectKey() != null && !change.previousObjectKey().equals(objectKey)) {
            deleteWithRetry(change.previousObjectKey(), "homepage-media-replace");
        }

        return new SiteMediaView(slot, storage.publicUrl(objectKey), change.updatedAt());
    }

    public SiteMediaView clear(SiteMediaSlot slot) {
        if (slot == null) throw slotNotFound();
        SiteMediaMetadataChange change = metadata.clear(slot);
        if (change.previousObjectKey() != null) {
            deleteWithRetry(change.previousObjectKey(), "homepage-media-clear");
        }
        return new SiteMediaView(slot, null, change.updatedAt());
    }

    private void deleteWithRetry(String objectKey, String operation) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= DELETE_ATTEMPTS; attempt++) {
            try {
                storage.delete(objectKey);
                return;
            } catch (RuntimeException exception) {
                last = exception;
                log.warn(
                        "S3 delete attempt failed operation={} objectKey={} attempt={}",
                        operation,
                        objectKey,
                        attempt
                );
            }
        }
        log.error(
                "S3 orphan cleanup required operation={} objectKey={} attempts={}",
                operation,
                objectKey,
                DELETE_ATTEMPTS,
                last
        );
    }

    private BusinessException emptyImage() {
        return new BusinessException(
                SiteContentErrorCodes.HOMEPAGE_MEDIA_EMPTY,
                "Homepage image file is empty."
        );
    }

    private BusinessException tooLarge() {
        return new BusinessException(
                SiteContentErrorCodes.HOMEPAGE_MEDIA_TOO_LARGE,
                "Homepage image exceeds the configured maximum size."
        );
    }

    private BusinessException slotNotFound() {
        return new BusinessException(
                SiteContentErrorCodes.HOMEPAGE_MEDIA_SLOT_NOT_FOUND,
                "Homepage media slot not found."
        );
    }
}
