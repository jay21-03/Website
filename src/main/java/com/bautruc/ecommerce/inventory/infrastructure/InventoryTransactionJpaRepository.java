package com.bautruc.ecommerce.inventory.infrastructure;
import java.util.Optional;import com.bautruc.ecommerce.inventory.domain.InventoryTransaction;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface InventoryTransactionJpaRepository extends JpaRepository<InventoryTransaction,Long>{Optional<InventoryTransaction> findByBusinessKey(String key);Page<InventoryTransaction> findByProductId(Long productId,Pageable pageable);}
