package com.bautruc.ecommerce.catalog.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bautruc.ecommerce.catalog.domain.ProductImage;
import com.bautruc.ecommerce.catalog.infrastructure.ProductImageJpaRepository;
import com.bautruc.ecommerce.catalog.infrastructure.ProductJpaRepository;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductImageMetadataService {
    public static final int MAX_IMAGES = 10;
    private static final short TEMPORARY_ORDER_START = 100;

    private final ProductJpaRepository products;
    private final ProductImageJpaRepository images;
    private final BusinessClock clock;

    public ProductImageMetadataService(ProductJpaRepository products, ProductImageJpaRepository images, BusinessClock clock) {
        this.products = products;
        this.images = images;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void preflightUpload(Long productId) {
        requireProduct(productId);
        if (images.countByProductId(productId) >= MAX_IMAGES) throw imageLimit();
    }

    @Transactional
    public ProductImage finalizeUpload(Long productId, String objectKey, String contentType, long size) {
        lockProduct(productId);
        List<ProductImage> current = images.findByProductIdOrderBySortOrderAscIdAsc(productId);
        if (current.size() >= MAX_IMAGES) throw imageLimit();
        return images.save(new ProductImage(productId, objectKey, contentType, size,
                (short) current.size(), current.isEmpty(), clock.now()));
    }

    @Transactional
    public String delete(Long productId, Long imageId) {
        lockProduct(productId);
        ProductImage target = requireImage(productId, imageId);
        List<ProductImage> current = images.findByProductIdOrderBySortOrderAscIdAsc(productId);
        if (target.isThumbnail()) {
            target.setThumbnail(false);
            images.flush();
            current.stream().filter(image -> !image.getId().equals(imageId)).findFirst()
                    .ifPresent(image -> image.setThumbnail(true));
        }
        images.delete(target);
        images.flush();
        normalizeOrder(current.stream().filter(image -> !image.getId().equals(imageId)).toList());
        return target.getObjectKey();
    }

    @Transactional
    public ProductImage setThumbnail(Long productId, Long imageId) {
        lockProduct(productId);
        List<ProductImage> current = images.findByProductIdOrderBySortOrderAscIdAsc(productId);
        ProductImage target = current.stream().filter(image -> image.getId().equals(imageId)).findFirst()
                .orElseThrow(this::imageNotFound);
        current.forEach(image -> image.setThumbnail(false));
        images.flush();
        target.setThumbnail(true);
        images.flush();
        return target;
    }

    @Transactional
    public List<ProductImage> reorder(Long productId, List<Long> requestedIds) {
        lockProduct(productId);
        List<ProductImage> current = images.findByProductIdOrderBySortOrderAscIdAsc(productId);
        validateExactIds(current, requestedIds);
        for (int index = 0; index < current.size(); index++) current.get(index).setSortOrder((short) (TEMPORARY_ORDER_START + index));
        images.flush();
        java.util.Map<Long, ProductImage> byId = current.stream().collect(java.util.stream.Collectors.toMap(ProductImage::getId, image -> image));
        for (int index = 0; index < requestedIds.size(); index++) byId.get(requestedIds.get(index)).setSortOrder((short) index);
        images.flush();
        return images.findByProductIdOrderBySortOrderAscIdAsc(productId);
    }

    private void normalizeOrder(List<ProductImage> remaining) {
        for (int index = 0; index < remaining.size(); index++) remaining.get(index).setSortOrder((short) (TEMPORARY_ORDER_START + index));
        images.flush();
        for (int index = 0; index < remaining.size(); index++) remaining.get(index).setSortOrder((short) index);
        images.flush();
    }

    private void validateExactIds(List<ProductImage> current, List<Long> requestedIds) {
        if (requestedIds == null || requestedIds.size() != current.size()) throw invalidOrder();
        Set<Long> requested = new HashSet<>(requestedIds);
        Set<Long> existing = current.stream().map(ProductImage::getId).collect(java.util.stream.Collectors.toSet());
        if (requested.size() != requestedIds.size() || !requested.equals(existing)) throw invalidOrder();
    }

    private void requireProduct(Long productId) {
        if (productId == null || products.findByIdAndDeletedAtIsNull(productId).isEmpty())
            throw new ResourceNotFoundException(CatalogErrorCodes.PRODUCT_NOT_FOUND, "Product not found.");
    }

    private void lockProduct(Long productId) {
        if (productId == null || products.findByIdAndDeletedAtIsNullForUpdate(productId).isEmpty())
            throw new ResourceNotFoundException(CatalogErrorCodes.PRODUCT_NOT_FOUND, "Product not found.");
    }

    private ProductImage requireImage(Long productId, Long imageId) {
        return images.findByIdAndProductId(imageId, productId).orElseThrow(this::imageNotFound);
    }

    private ResourceNotFoundException imageNotFound() {
        return new ResourceNotFoundException(CatalogErrorCodes.PRODUCT_IMAGE_NOT_FOUND, "Product image not found.");
    }

    private BusinessException imageLimit() {
        return new BusinessException(CatalogErrorCodes.PRODUCT_IMAGE_LIMIT_EXCEEDED, "A product can have at most 10 images.");
    }

    private BusinessException invalidOrder() {
        return new BusinessException(CatalogErrorCodes.PRODUCT_IMAGE_ORDER_INVALID,
                "Image IDs must contain every current product image exactly once.");
    }
}
