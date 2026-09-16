package com.bautruc.ecommerce.sitecontent.infrastructure;

import java.util.Optional;

import com.bautruc.ecommerce.sitecontent.domain.SiteMedia;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiteMediaJpaRepository extends JpaRepository<SiteMedia, Long> {
    Optional<SiteMedia> findBySlot(SiteMediaSlot slot);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select media from SiteMedia media where media.slot = :slot")
    Optional<SiteMedia> findBySlotForUpdate(@Param("slot") SiteMediaSlot slot);
}
