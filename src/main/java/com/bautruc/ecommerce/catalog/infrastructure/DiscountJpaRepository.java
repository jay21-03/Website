package com.bautruc.ecommerce.catalog.infrastructure;
import java.util.Optional;
import com.bautruc.ecommerce.catalog.domain.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DiscountJpaRepository extends JpaRepository<Discount,Long>{Optional<Discount> findByProductId(Long productId);void deleteByProductId(Long productId);}
