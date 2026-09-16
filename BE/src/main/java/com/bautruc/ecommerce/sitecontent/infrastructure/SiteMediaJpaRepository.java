package com.bautruc.ecommerce.sitecontent.infrastructure;

import java.util.Optional;
import com.bautruc.ecommerce.sitecontent.domain.SiteMedia;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteMediaJpaRepository extends JpaRepository<SiteMedia, Long> {
    Optional<SiteMedia> findBySlot(SiteMediaSlot slot);
}
