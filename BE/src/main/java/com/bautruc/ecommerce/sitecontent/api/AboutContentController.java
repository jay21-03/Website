package com.bautruc.ecommerce.sitecontent.api;

import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.logging.LogContext;
import com.bautruc.ecommerce.common.response.ApiResponse;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.sitecontent.api.request.AboutContentRequest;
import com.bautruc.ecommerce.sitecontent.api.response.AboutContentResponse;
import com.bautruc.ecommerce.sitecontent.api.response.HomepageMediaResponse;
import com.bautruc.ecommerce.sitecontent.application.AboutContentService;
import com.bautruc.ecommerce.sitecontent.application.SiteMediaService;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AboutContentController {

    private final AboutContentService content;
    private final SiteMediaService media;
    private final BusinessClock clock;

    public AboutContentController(
            AboutContentService content,
            SiteMediaService media,
            BusinessClock clock
    ) {
        this.content = content;
        this.media = media;
        this.clock = clock;
    }

    @GetMapping("/api/v1/about")
    public ApiResponse<AboutContentResponse> publicAbout() {
        return ok(content.current());
    }

    @GetMapping("/api/v1/admin/about")
    public ApiResponse<AboutContentResponse> adminAbout() {
        return ok(content.current());
    }

    @PutMapping("/api/v1/admin/about")
    public ApiResponse<AboutContentResponse> update(
            @Valid @RequestBody AboutContentRequest request
    ) {
        return ok(content.save(request));
    }

    @PutMapping(
            value = "/api/v1/admin/about/media/{slot}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<HomepageMediaResponse> upload(
            @PathVariable SiteMediaSlot slot,
            @RequestPart("file") MultipartFile file
    ) {
        requireAboutSlot(slot);
        return ok(HomepageMediaResponse.from(media.upload(slot, file)));
    }

    @DeleteMapping("/api/v1/admin/about/media/{slot}")
    public ApiResponse<HomepageMediaResponse> clear(
            @PathVariable SiteMediaSlot slot
    ) {
        requireAboutSlot(slot);
        return ok(HomepageMediaResponse.from(media.clear(slot)));
    }

    private void requireAboutSlot(SiteMediaSlot slot) {
        if (slot == null || !slot.isAbout()) {
            throw new BusinessException(
                    "ABOUT_MEDIA_SLOT_INVALID",
                    "Invalid About page media slot.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(
                data,
                null,
                clock.businessNow().toOffsetDateTime(),
                LogContext.currentCorrelationId()
        );
    }
}
