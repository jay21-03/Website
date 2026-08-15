package com.bautruc.ecommerce.catalog.application;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.bautruc.ecommerce.catalog.domain.ProductImage;
import com.bautruc.ecommerce.catalog.infrastructure.ProductImageJpaRepository;
import com.bautruc.ecommerce.common.config.ApplicationProperties;
import com.bautruc.ecommerce.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageService {
    private static final Logger log = LoggerFactory.getLogger(ProductImageService.class);
    private static final int DELETE_ATTEMPTS = 3;

    private final ProductImageMetadataService metadata;
    private final ProductImageJpaRepository images;
    private final ObjectStoragePort storage;
    private final ImageSignatureValidator signatureValidator;
    private final long maxBytes;

    public ProductImageService(ProductImageMetadataService metadata, ProductImageJpaRepository images,
                               ObjectStoragePort storage, ImageSignatureValidator signatureValidator,
                               ApplicationProperties properties) {
        this.metadata = metadata;
        this.images = images;
        this.storage = storage;
        this.signatureValidator = signatureValidator;
        this.maxBytes = properties.image() == null || properties.image().maxBytes() <= 0
                ? 5L * 1024 * 1024 : properties.image().maxBytes();
    }

    public ProductImage upload(Long productId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw emptyImage();
        if (file.getSize() > maxBytes) throw tooLarge();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(CatalogErrorCodes.PRODUCT_IMAGE_SIGNATURE_INVALID,
                    "Could not read uploaded image.");
        }
        if (bytes.length > maxBytes) throw tooLarge();
        ValidatedImage validated = signatureValidator.validate(bytes, file.getContentType());
        metadata.preflightUpload(productId);
        String objectKey = "products/" + productId + "/" + UUID.randomUUID() + "." + validated.extension();
        try {
            storage.put(objectKey, validated.content(), validated.contentType());
        } catch (ObjectStorageException exception) {
            throw new BusinessException(CatalogErrorCodes.S3_UPLOAD_FAILED,
                    "Image storage upload failed.", HttpStatus.BAD_GATEWAY);
        }
        try {
            return metadata.finalizeUpload(productId, objectKey, validated.contentType(), validated.content().length);
        } catch (RuntimeException exception) {
            deleteWithRetry(objectKey, "upload-compensation");
            throw exception;
        }
    }

    public void delete(Long productId, Long imageId) {
        String objectKey = metadata.delete(productId, imageId);
        deleteWithRetry(objectKey, "image-delete");
    }

    public ProductImage setThumbnail(Long productId, Long imageId) {
        return metadata.setThumbnail(productId, imageId);
    }

    public List<ProductImage> reorder(Long productId, List<Long> imageIds) {
        return metadata.reorder(productId, imageIds);
    }

    @Transactional(readOnly = true)
    public List<ProductImage> images(Long productId) {
        return images.findByProductIdOrderBySortOrderAscIdAsc(productId);
    }

    @Transactional(readOnly = true)
    public String thumbnailUrl(Long productId) {
        return images.findFirstByProductIdAndThumbnailTrue(productId)
                .map(ProductImage::getObjectKey).map(storage::publicUrl).orElse(null);
    }

    public String publicUrl(ProductImage image) {
        return storage.publicUrl(image.getObjectKey());
    }

    private void deleteWithRetry(String objectKey, String operation) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= DELETE_ATTEMPTS; attempt++) {
            try {
                storage.delete(objectKey);
                return;
            } catch (RuntimeException exception) {
                last = exception;
                log.warn("S3 delete attempt failed operation={} objectKey={} attempt={}", operation, objectKey, attempt);
            }
        }
        log.error("S3 orphan cleanup required operation={} objectKey={} attempts={}", operation, objectKey,
                DELETE_ATTEMPTS, last);
    }

    private BusinessException emptyImage() {
        return new BusinessException(CatalogErrorCodes.PRODUCT_IMAGE_EMPTY, "Image file is empty.");
    }

    private BusinessException tooLarge() {
        return new BusinessException(CatalogErrorCodes.PRODUCT_IMAGE_TOO_LARGE,
                "Image exceeds the configured maximum size.");
    }
}
