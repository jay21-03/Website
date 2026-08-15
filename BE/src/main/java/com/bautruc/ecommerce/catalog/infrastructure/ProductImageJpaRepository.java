package com.bautruc.ecommerce.catalog.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.bautruc.ecommerce.catalog.domain.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageJpaRepository extends JpaRepository<ProductImage, Long> {
    long countByProductId(Long productId);
    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);
    Optional<ProductImage> findByIdAndProductId(Long id, Long productId);
    Optional<ProductImage> findFirstByProductIdAndThumbnailTrue(Long productId);
    List<ProductImage> findByProductIdInAndThumbnailTrue(Collection<Long> productIds);
}
