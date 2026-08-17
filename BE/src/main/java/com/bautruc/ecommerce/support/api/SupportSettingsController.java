package com.bautruc.ecommerce.support.api;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.support.api.request.SupportSettingsRequest;
import com.bautruc.ecommerce.support.api.response.SupportSettingsResponse;
import com.bautruc.ecommerce.support.application.SupportSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SupportSettingsController {
    private final SupportSettingsService service;
    private final BusinessClock businessClock;

    public SupportSettingsController(SupportSettingsService service, BusinessClock businessClock) {
        this.service = service;
        this.businessClock = businessClock;
    }

    @GetMapping("/api/v1/support/settings")
    public ApiResponse<SupportSettingsResponse> current() {
        return ok(SupportSettingsResponse.from(service.current()));
    }

    @GetMapping("/api/v1/admin/support/settings")
    public ApiResponse<SupportSettingsResponse> adminCurrent() {
        return current();
    }

    @PutMapping("/api/v1/admin/support/settings")
    public ApiResponse<SupportSettingsResponse> update(@Valid @RequestBody SupportSettingsRequest request) {
        return ok(SupportSettingsResponse.from(service.update(request)));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, null, businessClock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
