package com.bautruc.ecommerce.workshop.api;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.response.PageResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.workshop.api.request.WorkshopBookingRequest;
import com.bautruc.ecommerce.workshop.api.request.WorkshopBookingStatusRequest;
import com.bautruc.ecommerce.workshop.api.response.WorkshopBookingResponse;
import com.bautruc.ecommerce.workshop.application.WorkshopBookingService;
import com.bautruc.ecommerce.workshop.domain.WorkshopBookingStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkshopBookingController {
    private final WorkshopBookingService service;
    private final BusinessClock businessClock;

    public WorkshopBookingController(WorkshopBookingService service, BusinessClock businessClock) {
        this.service = service;
        this.businessClock = businessClock;
    }

    @PostMapping("/api/v1/workshop/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkshopBookingResponse> create(@Valid @RequestBody WorkshopBookingRequest request) {
        return ok(WorkshopBookingResponse.from(service.create(request)));
    }

    @GetMapping("/api/v1/admin/workshop/bookings")
    public ApiResponse<PageResponse<WorkshopBookingResponse>> list(
            @RequestParam(required = false) WorkshopBookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ok(PageResponse.from(service.list(status, page, size).map(WorkshopBookingResponse::from)));
    }

    @PatchMapping("/api/v1/admin/workshop/bookings/{id}/status")
    public ApiResponse<WorkshopBookingResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody WorkshopBookingStatusRequest request
    ) {
        return ok(WorkshopBookingResponse.from(service.updateStatus(id, request)));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(
                data,
                null,
                businessClock.businessNow().toOffsetDateTime(),
                LogContext.currentCorrelationId()
        );
    }
}
