package com.bautruc.ecommerce.sitecontent.infrastructure;

import com.bautruc.ecommerce.sitecontent.domain.HomepageContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomepageContentJpaRepository extends JpaRepository<HomepageContent, Short> {
}
