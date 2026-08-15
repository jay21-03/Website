package com.bautruc.ecommerce.catalog.infrastructure;
import java.util.*;
import com.bautruc.ecommerce.catalog.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CollectionJpaRepository extends JpaRepository<ProductCollection,Long>{
    Optional<ProductCollection> findByIdAndDeletedAtIsNull(Long id);
    List<ProductCollection> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(CollectionStatus status);
}
