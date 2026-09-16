package com.bautruc.ecommerce.sitecontent.infrastructure;

import java.util.List;
import com.bautruc.ecommerce.sitecontent.domain.HomepageFeaturedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomepageFeaturedProductJpaRepository extends JpaRepository<HomepageFeaturedProduct, Short> {
    List<HomepageFeaturedProduct> findAllByOrderBySlotAsc();
}
