package com.bautruc.ecommerce.catalog.infrastructure;
import java.util.*;
import com.bautruc.ecommerce.catalog.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface ProductJpaRepository extends JpaRepository<Product,Long>, JpaSpecificationExecutor<Product>{
    Optional<Product> findByIdAndDeletedAtIsNull(Long id);
    Page<Product> findByStatusAndDeletedAtIsNull(ProductStatus status,Pageable pageable);
    Page<Product> findByDeletedAtIsNull(Pageable pageable);
    boolean existsByCollectionIdAndDeletedAtIsNull(Long collectionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id=:id and p.deletedAt is null")
    Optional<Product> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);
}
