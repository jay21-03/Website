package com.bautruc.ecommerce.workshop.application;

import java.util.List;
import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.workshop.api.request.WorkshopOfferingRequest;
import com.bautruc.ecommerce.workshop.domain.WorkshopOffering;
import com.bautruc.ecommerce.workshop.domain.WorkshopOfferingStatus;
import com.bautruc.ecommerce.workshop.infrastructure.WorkshopOfferingJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkshopOfferingService {
    private final WorkshopOfferingJpaRepository repository;
    private final BusinessClock businessClock;

    public WorkshopOfferingService(WorkshopOfferingJpaRepository repository, BusinessClock businessClock) {
        this.repository = repository;
        this.businessClock = businessClock;
    }

    @Transactional(readOnly = true)
    public List<WorkshopOffering> publicOfferings() {
        return repository.findByStatusOrderByCreatedAtDesc(WorkshopOfferingStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<WorkshopOffering> adminOfferings() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public WorkshopOffering detail(Long id) {
        return require(id);
    }

    @Transactional
    public WorkshopOffering create(WorkshopOfferingRequest request) {
        return repository.save(new WorkshopOffering(
                request.title(),
                request.description(),
                request.priceAmount(),
                request.durationMinutes(),
                request.maxParticipants(),
                request.imageUrl(),
                request.status(),
                businessClock.now()
        ));
    }

    @Transactional
    public WorkshopOffering update(Long id, WorkshopOfferingRequest request) {
        WorkshopOffering offering = require(id);
        offering.update(
                request.title(),
                request.description(),
                request.priceAmount(),
                request.durationMinutes(),
                request.maxParticipants(),
                request.imageUrl(),
                request.status(),
                businessClock.now()
        );
        return offering;
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(require(id));
    }

    WorkshopOffering require(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(WorkshopErrorCodes.WORKSHOP_NOT_FOUND, "Workshop not found."));
    }
}
