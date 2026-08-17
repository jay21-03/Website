package com.bautruc.ecommerce.workshop.infrastructure;

import java.util.List;
import com.bautruc.ecommerce.workshop.domain.WorkshopOffering;
import com.bautruc.ecommerce.workshop.domain.WorkshopOfferingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopOfferingJpaRepository extends JpaRepository<WorkshopOffering, Long> {
    List<WorkshopOffering> findByStatusOrderByCreatedAtDesc(WorkshopOfferingStatus status);
}
