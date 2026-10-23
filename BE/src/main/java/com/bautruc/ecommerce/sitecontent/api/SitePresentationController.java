package com.bautruc.ecommerce.sitecontent.api;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.sitecontent.api.request.SitePresentationRequest;
import com.bautruc.ecommerce.sitecontent.api.response.HomepageMediaResponse;
import com.bautruc.ecommerce.sitecontent.api.response.SitePresentationResponse;
import com.bautruc.ecommerce.sitecontent.application.SiteMediaService;
import com.bautruc.ecommerce.sitecontent.application.SitePresentationService;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SitePresentationController {
    private final SitePresentationService presentation;
    private final SiteMediaService media;
    private final BusinessClock clock;

    public SitePresentationController(SitePresentationService presentation, SiteMediaService media, BusinessClock clock) {
        this.presentation = presentation;
        this.media = media;
        this.clock = clock;
    }

    @GetMapping("/api/v1/site/presentation")
    public ApiResponse<SitePresentationResponse> publicSettings() { return ok(presentation.current()); }

    @GetMapping("/api/v1/admin/site/presentation")
    public ApiResponse<SitePresentationResponse> adminSettings() { return ok(presentation.current()); }

    @PutMapping("/api/v1/admin/site/presentation")
    public ApiResponse<SitePresentationResponse> update(@Valid @RequestBody SitePresentationRequest request) {
        return ok(presentation.save(request));
    }

    @PutMapping(value = "/api/v1/admin/site/media/{slot}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<HomepageMediaResponse> upload(@PathVariable SiteMediaSlot slot,
                                                     @RequestPart("file") MultipartFile file) {
        requireSiteSlot(slot);
        return ok(HomepageMediaResponse.from(media.upload(slot, file)));
    }

    @DeleteMapping("/api/v1/admin/site/media/{slot}")
    public ApiResponse<HomepageMediaResponse> clear(@PathVariable SiteMediaSlot slot) {
        requireSiteSlot(slot);
        return ok(HomepageMediaResponse.from(media.clear(slot)));
    }

    private void requireSiteSlot(SiteMediaSlot slot) {
        if (slot != SiteMediaSlot.CATALOG_HERO && slot != SiteMediaSlot.CATALOG_HERO_DARK &&
                slot != SiteMediaSlot.WORKSHOP_HERO && slot != SiteMediaSlot.WORKSHOP_HERO_DARK &&
                slot != SiteMediaSlot.SUPPORT_HERO && slot != SiteMediaSlot.SUPPORT_HERO_DARK &&
                slot != SiteMediaSlot.CHECKOUT_HERO && slot != SiteMediaSlot.CHECKOUT_HERO_DARK &&
                slot != SiteMediaSlot.SITE_LOGO && slot != SiteMediaSlot.SITE_LOGO_DARK &&
                slot != SiteMediaSlot.SITE_CHAT_ICON && slot != SiteMediaSlot.SITE_ZALO_ICON &&
                slot != SiteMediaSlot.SITE_FACEBOOK_ICON && slot != SiteMediaSlot.SITE_TRUST_1_ICON &&
                slot != SiteMediaSlot.SITE_TRUST_2_ICON && slot != SiteMediaSlot.SITE_TRUST_3_ICON) {
            throw new BusinessException("SITE_MEDIA_SLOT_INVALID", "Invalid site media slot.");
        }
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, null, clock.businessNow().toOffsetDateTime(),
                LogContext.currentCorrelationId());
    }
}
