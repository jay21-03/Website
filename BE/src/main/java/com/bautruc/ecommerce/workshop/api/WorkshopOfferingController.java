package com.bautruc.ecommerce.workshop.api;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.workshop.api.request.WorkshopOfferingRequest;
import com.bautruc.ecommerce.workshop.api.response.WorkshopOfferingResponse;
import com.bautruc.ecommerce.workshop.application.WorkshopOfferingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkshopOfferingController {
    private final WorkshopOfferingService service;
    private final BusinessClock businessClock;

    public WorkshopOfferingController(WorkshopOfferingService service, BusinessClock businessClock) {
        this.service = service;
        this.businessClock = businessClock;
    }

    @GetMapping("/api/v1/workshops")
    public ApiResponse<List<WorkshopOfferingResponse>> publicWorkshops() {
        return ok(service.publicOfferings().stream().map(WorkshopOfferingResponse::from).toList());
    }

    @GetMapping("/api/v1/workshops/{id}")
    public ApiResponse<WorkshopOfferingResponse> publicWorkshop(@PathVariable Long id) {
        var offering = service.detail(id);
        if (!offering.isBookable()) {
            throw new com.bautruc.ecommerce.common.exception.ResourceNotFoundException(
                    com.bautruc.ecommerce.workshop.application.WorkshopErrorCodes.WORKSHOP_NOT_FOUND,
                    "Workshop not found."
            );
        }
        return ok(WorkshopOfferingResponse.from(offering));
    }

    @GetMapping("/api/v1/admin/workshops")
    public ApiResponse<List<WorkshopOfferingResponse>> adminWorkshops() {
        return ok(service.adminOfferings().stream().map(WorkshopOfferingResponse::from).toList());
    }

    @PostMapping("/api/v1/admin/workshops")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkshopOfferingResponse> create(@Valid @RequestBody WorkshopOfferingRequest request) {
        return ok(WorkshopOfferingResponse.from(service.create(request)));
    }

    @PutMapping("/api/v1/admin/workshops/{id}")
    public ApiResponse<WorkshopOfferingResponse> update(@PathVariable Long id, @Valid @RequestBody WorkshopOfferingRequest request) {
        return ok(WorkshopOfferingResponse.from(service.update(id, request)));
    }

    @DeleteMapping("/api/v1/admin/workshops/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ok(null);
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, null, businessClock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
