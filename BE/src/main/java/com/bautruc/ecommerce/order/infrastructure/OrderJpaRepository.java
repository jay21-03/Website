package com.bautruc.ecommerce.order.infrastructure;
import java.util.Optional;import com.bautruc.ecommerce.order.domain.Order;import jakarta.persistence.LockModeType;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface OrderJpaRepository extends JpaRepository<Order,Long>,JpaSpecificationExecutor<Order>{
 @Query(value="select nextval('app_global_id_seq')",nativeQuery=true)Long nextId();Page<Order> findByUserId(Long userId,Pageable pageable);Optional<Order> findByIdAndUserId(Long id,Long userId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select o from Order o where o.id=:id")Optional<Order> findByIdForUpdate(@Param("id")Long id);
}
