package com.bautruc.ecommerce.sitecontent.application;

import java.time.Instant;

import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;

public record SiteMediaMetadataChange(
        SiteMediaSlot slot,
        String previousObjectKey,
        String objectKey,
        Instant updatedAt
) {
}
