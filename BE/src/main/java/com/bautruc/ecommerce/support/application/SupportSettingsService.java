package com.bautruc.ecommerce.support.application;

import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.support.api.request.SupportSettingsRequest;
import com.bautruc.ecommerce.support.domain.SupportSettings;
import com.bautruc.ecommerce.support.infrastructure.SupportSettingsJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportSettingsService {
    private final SupportSettingsJpaRepository repository;
    private final BusinessClock businessClock;

    public SupportSettingsService(SupportSettingsJpaRepository repository, BusinessClock businessClock) {
        this.repository = repository;
        this.businessClock = businessClock;
    }

    @Transactional(readOnly = true)
    public SupportSettings current() {
        return repository.findById(SupportSettings.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("SUPPORT_SETTINGS_NOT_FOUND", "Support settings not found."));
    }

    @Transactional
    public SupportSettings update(SupportSettingsRequest request) {
        SupportSettings settings = current();
        settings.update(
                request.email(),
                request.zaloPhone(),
                request.secondaryPhone(),
                request.facebookUrl(),
                request.address(),
                request.mapUrl(),
                request.openingHours(),
                businessClock.now()
        );
        return settings;
    }
}
