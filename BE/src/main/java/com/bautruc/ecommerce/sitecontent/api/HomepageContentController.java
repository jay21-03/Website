package com.bautruc.ecommerce.sitecontent.api;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.sitecontent.api.request.UpdateFeaturedProductsRequest;
import com.bautruc.ecommerce.sitecontent.api.response.AdminHomepageResponse;
import com.bautruc.ecommerce.sitecontent.api.response.HomepageResponse;
import com.bautruc.ecommerce.sitecontent.application.HomepageContentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomepageContentController {
    private final HomepageContentService service;
    private final BusinessClock clock;

    public HomepageContentController(HomepageContentService service, BusinessClock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping("/api/v1/home")
    public ApiResponse<HomepageResponse> home() {
        return ok(HomepageResponse.from(service.current()));
    }

    @GetMapping("/api/v1/admin/home")
    public ApiResponse<AdminHomepageResponse> adminHome() {
        return ok(AdminHomepageResponse.from(service.current()));
    }

    @PutMapping("/api/v1/admin/home/featured-products")
    public ApiResponse<AdminHomepageResponse> updateFeaturedProducts(
            @Valid @RequestBody UpdateFeaturedProductsRequest request
    ) {
        return ok(AdminHomepageResponse.from(service.updateFeaturedProducts(request.productIds())));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, null, clock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
