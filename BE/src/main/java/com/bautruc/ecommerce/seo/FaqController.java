package com.bautruc.ecommerce.seo;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaqController {
    private final FaqService service;
    private final BusinessClock clock;
    public FaqController(FaqService service, BusinessClock clock) { this.service = service; this.clock = clock; }
    @GetMapping("/api/v1/faq") public ApiResponse<List<FaqService.Item>> publicItems() { return ok(service.publicItems()); }
    @GetMapping("/api/v1/admin/faq") public ApiResponse<List<FaqService.Item>> adminItems() { return ok(service.adminItems()); }
    @PostMapping("/api/v1/admin/faq") public ApiResponse<FaqService.Item> create(@Valid @RequestBody FaqService.Input value) { return ok(service.save(null, value)); }
    @PutMapping("/api/v1/admin/faq/{id}") public ApiResponse<FaqService.Item> update(@PathVariable Long id,
            @Valid @RequestBody FaqService.Input value) { return ok(service.save(id, value)); }
    @DeleteMapping("/api/v1/admin/faq/{id}") public ApiResponse<Void> delete(@PathVariable Long id) { service.delete(id); return ok(null); }
    private <T> ApiResponse<T> ok(T value) {
        return ApiResponse.success(value, null, clock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
