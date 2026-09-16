package com.bautruc.ecommerce.sitecontent.application;

import java.time.Instant;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;

public record SiteMediaView(
        SiteMediaSlot slot,
        String url,
        Instant updatedAt
) {
}
