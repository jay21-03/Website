package com.bautruc.ecommerce.workshop.infrastructure;

import com.bautruc.ecommerce.workshop.domain.WorkshopBooking;
import com.bautruc.ecommerce.workshop.domain.WorkshopBookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopBookingJpaRepository extends JpaRepository<WorkshopBooking, Long> {
    Page<WorkshopBooking> findByStatus(WorkshopBookingStatus status, Pageable pageable);
}
