package com.bautruc.ecommerce.catalog.infrastructure;
import java.util.*;
import com.bautruc.ecommerce.catalog.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CollectionJpaRepository extends JpaRepository<ProductCollection,Long>{
    Optional<ProductCollection> findByIdAndDeletedAtIsNull(Long id);
    Page<ProductCollection> findByDeletedAtIsNull(Pageable pageable);
    List<ProductCollection> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(CollectionStatus status);
}
