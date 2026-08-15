package com.bautruc.ecommerce.inventory.infrastructure;
import java.util.*;import com.bautruc.ecommerce.inventory.domain.Inventory;import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface InventoryJpaRepository extends JpaRepository<Inventory,Long>,JpaSpecificationExecutor<Inventory>{
 Optional<Inventory> findByProductId(Long productId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from Inventory i where i.productId=:productId") Optional<Inventory> findByProductIdForUpdate(@Param("productId")Long productId);
}
