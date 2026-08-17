package com.bautruc.ecommerce.workshop.application;

import java.time.Instant;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.common.response.PageResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.workshop.api.request.WorkshopBookingRequest;
import com.bautruc.ecommerce.workshop.api.request.WorkshopBookingStatusRequest;
import com.bautruc.ecommerce.workshop.domain.WorkshopBooking;
import com.bautruc.ecommerce.workshop.domain.WorkshopBookingStatus;
import com.bautruc.ecommerce.workshop.domain.WorkshopOffering;
import com.bautruc.ecommerce.workshop.infrastructure.WorkshopBookingJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkshopBookingService {
    private final WorkshopBookingJpaRepository repository;
    private final WorkshopOfferingService offeringService;
    private final BusinessClock businessClock;

    public WorkshopBookingService(WorkshopBookingJpaRepository repository, WorkshopOfferingService offeringService, BusinessClock businessClock) {
        this.repository = repository;
        this.offeringService = offeringService;
        this.businessClock = businessClock;
    }

    @Transactional
    public WorkshopBooking create(WorkshopBookingRequest request) {
        Instant now = businessClock.now();
        Instant preferredAt = request.preferredAt().toInstant();
        if (!preferredAt.isAfter(now)) {
            throw new BusinessException(
                    WorkshopErrorCodes.WORKSHOP_INVALID_SCHEDULE,
                    "Workshop booking time must be in the future."
            );
        }
        if (request.workshopId() != null) {
            WorkshopOffering offering = offeringService.require(request.workshopId());
            if (!offering.isBookable()) {
                throw new BusinessException(
                        WorkshopErrorCodes.WORKSHOP_NOT_BOOKABLE,
                        "Workshop is not available for booking."
                );
            }
            if (request.participants() > offering.getMaxParticipants()) {
                throw new BusinessException(
                        WorkshopErrorCodes.WORKSHOP_INVALID_SCHEDULE,
                        "Participants exceed workshop capacity."
                );
            }
        }
        WorkshopBooking booking = new WorkshopBooking(
                request.fullName(),
                request.workshopId(),
                request.email(),
                request.phone(),
                preferredAt,
                request.participants(),
                request.note(),
                now
        );
        return repository.save(booking);
    }

    @Transactional(readOnly = true)
    public Page<WorkshopBooking> list(WorkshopBookingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, PageResponse.DEFAULT_PAGE),
                Math.min(Math.max(size, 1), PageResponse.MAX_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
    }

    @Transactional
    public WorkshopBooking updateStatus(Long id, WorkshopBookingStatusRequest request) {
        WorkshopBooking booking = repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(
                        WorkshopErrorCodes.WORKSHOP_BOOKING_NOT_FOUND,
                        "Workshop booking not found."
                ));
        booking.changeStatus(request.status(), businessClock.now());
        return booking;
    }
}
