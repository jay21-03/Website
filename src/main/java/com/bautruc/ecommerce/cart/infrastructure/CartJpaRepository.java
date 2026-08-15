package com.bautruc.ecommerce.cart.infrastructure;
import java.time.Instant;import java.util.Optional;import com.bautruc.ecommerce.cart.domain.Cart;import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
public interface CartJpaRepository extends JpaRepository<Cart,Long>{
 Optional<Cart> findByUserId(Long userId);
 @Transactional @Modifying @Query(value="INSERT INTO carts(id,user_id,created_at,updated_at) VALUES(nextval('app_global_id_seq'),:userId,:now,:now) ON CONFLICT (user_id) DO NOTHING",nativeQuery=true)int ensureCart(@Param("userId")Long userId,@Param("now")Instant now);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Cart c where c.userId=:userId")Optional<Cart> findByUserIdForUpdate(@Param("userId")Long userId);
}
