package com.bautruc.ecommerce.sitecontent.api;

import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.sitecontent.api.request.UpdateFeaturedProductsRequest;
import com.bautruc.ecommerce.sitecontent.api.request.UpdateHomepageCopyRequest;
import com.bautruc.ecommerce.sitecontent.api.request.UpdateHomepageSloganRequest;
import com.bautruc.ecommerce.sitecontent.api.response.AdminHomepageResponse;
import com.bautruc.ecommerce.sitecontent.api.response.HomepageMediaResponse;
import com.bautruc.ecommerce.sitecontent.api.response.HomepageResponse;
import com.bautruc.ecommerce.sitecontent.application.HomepageContentService;
import com.bautruc.ecommerce.sitecontent.application.SiteMediaService;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class HomepageContentController {
    private final HomepageContentService service;
    private final SiteMediaService mediaService;
    private final BusinessClock clock;

    public HomepageContentController(
            HomepageContentService service,
            SiteMediaService mediaService,
            BusinessClock clock
    ) {
        this.service = service;
        this.mediaService = mediaService;
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

    @PutMapping("/api/v1/admin/home/slogan")
    public ApiResponse<AdminHomepageResponse> updateSlogan(
            @Valid @RequestBody UpdateHomepageSloganRequest request
    ) {
        return ok(AdminHomepageResponse.from(service.updateSlogan(request.sloganVi(), request.sloganEn())));
    }

    @PutMapping("/api/v1/admin/home/copy")
    public ApiResponse<AdminHomepageResponse> updateCopy(
            @Valid @RequestBody UpdateHomepageCopyRequest request
    ) {
        return ok(AdminHomepageResponse.from(service.updateCopy(request.copy())));
    }

    @PutMapping(value = "/api/v1/admin/home/media/{slot}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<HomepageMediaResponse> uploadMedia(
            @PathVariable SiteMediaSlot slot,
            @RequestPart("file") MultipartFile file
    ) {
        return ok(HomepageMediaResponse.from(mediaService.upload(slot, file)));
    }

    @DeleteMapping("/api/v1/admin/home/media/{slot}")
    public ApiResponse<HomepageMediaResponse> clearMedia(@PathVariable SiteMediaSlot slot) {
        return ok(HomepageMediaResponse.from(mediaService.clear(slot)));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, null, clock.businessNow().toOffsetDateTime(), LogContext.currentCorrelationId());
    }
}
