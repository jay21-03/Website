package com.bautruc.ecommerce.order.infrastructure;
import java.util.List;import com.bautruc.ecommerce.order.domain.OrderItem;import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderItemJpaRepository extends JpaRepository<OrderItem,Long>{List<OrderItem> findByOrderIdOrderById(Long orderId);}
