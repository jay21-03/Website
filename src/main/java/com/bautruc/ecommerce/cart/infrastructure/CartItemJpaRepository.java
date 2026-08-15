package com.bautruc.ecommerce.cart.infrastructure;
import java.util.*;import com.bautruc.ecommerce.cart.domain.CartItem;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface CartItemJpaRepository extends JpaRepository<CartItem,Long>{
 List<CartItem> findByCartIdOrderByCreatedAtAsc(Long cartId);Optional<CartItem> findByCartIdAndProductId(Long cartId,Long productId);Optional<CartItem> findByIdAndCartId(Long id,Long cartId);
 @Modifying @Query("delete from CartItem i where i.id=:id and i.cartId=:cartId and i.version=:version")int deleteSnapshot(@Param("id")Long id,@Param("cartId")Long cartId,@Param("version")long version);
}
