package com.bautruc.ecommerce.sitecontent.api.response;

import java.time.Instant;
import com.bautruc.ecommerce.sitecontent.application.SiteMediaView;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;

public record HomepageMediaResponse(
        SiteMediaSlot slot,
        String url,
        Instant updatedAt
) {
    public static HomepageMediaResponse from(SiteMediaView media) {
        return new HomepageMediaResponse(media.slot(), media.url(), media.updatedAt());
    }
}
