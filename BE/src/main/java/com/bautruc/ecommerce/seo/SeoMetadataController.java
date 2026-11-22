package com.bautruc.ecommerce.seo;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoMetadataController {
    private final SeoMetadataService service;
    private final BusinessClock clock;

    public SeoMetadataController(SeoMetadataService service, BusinessClock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping("/api/v1/admin/seo/metadata")
    public ApiResponse<Map<String, SeoMetadataService.Metadata>> all() { return ok(service.all()); }

    @GetMapping("/api/v1/seo/metadata")
    public ApiResponse<Map<String, SeoMetadataService.Metadata>> publicMetadata() { return ok(service.all()); }

    @PutMapping("/api/v1/admin/seo/metadata")
    public ApiResponse<SeoMetadataService.Metadata> update(@Valid @RequestBody SeoMetadataService.Metadata value) {
        return ok(service.save(value));
    }

    private <T> ApiResponse<T> ok(T value) {
        return ApiResponse.success(value, null, clock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
