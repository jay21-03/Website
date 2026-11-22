package com.bautruc.ecommerce.catalog.infrastructure;
import java.util.*;
import com.bautruc.ecommerce.catalog.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CollectionJpaRepository extends JpaRepository<ProductCollection,Long>, JpaSpecificationExecutor<ProductCollection>{
    Optional<ProductCollection> findByIdAndDeletedAtIsNull(Long id);
    Optional<ProductCollection> findBySlugAndStatusAndDeletedAtIsNull(String slug, CollectionStatus status);
    boolean existsBySlug(String slug);
    Page<ProductCollection> findByDeletedAtIsNull(Pageable pageable);
    List<ProductCollection> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(CollectionStatus status);
}
