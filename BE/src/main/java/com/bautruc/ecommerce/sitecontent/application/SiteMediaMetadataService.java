package com.bautruc.ecommerce.sitecontent.application;

import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;
import com.bautruc.ecommerce.common.time.BusinessClock;
import com.bautruc.ecommerce.sitecontent.domain.SiteMedia;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;
import com.bautruc.ecommerce.sitecontent.infrastructure.SiteMediaJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteMediaMetadataService {
    private final SiteMediaJpaRepository media;
    private final BusinessClock clock;

    public SiteMediaMetadataService(SiteMediaJpaRepository media, BusinessClock clock) {
        this.media = media;
        this.clock = clock;
    }

    @Transactional
    public SiteMediaMetadataChange replace(
            SiteMediaSlot slot,
            String objectKey,
            String contentType,
            long fileSizeBytes
    ) {
        SiteMedia item = requireForUpdate(slot);
        String previousObjectKey = item.getObjectKey();
        item.replace(objectKey, contentType, fileSizeBytes, clock.now());
        media.flush();
        return new SiteMediaMetadataChange(slot, previousObjectKey, item.getObjectKey(), item.getUpdatedAt());
    }

    @Transactional
    public SiteMediaMetadataChange clear(SiteMediaSlot slot) {
        SiteMedia item = requireForUpdate(slot);
        String previousObjectKey = item.getObjectKey();
        item.clear(clock.now());
        media.flush();
        return new SiteMediaMetadataChange(slot, previousObjectKey, null, item.getUpdatedAt());
    }

    private SiteMedia requireForUpdate(SiteMediaSlot slot) {
        if (slot == null) {
            throw notFound();
        }
        return media.findBySlotForUpdate(slot).orElseThrow(this::notFound);
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException(
                SiteContentErrorCodes.HOMEPAGE_MEDIA_SLOT_NOT_FOUND,
                "Homepage media slot not found."
        );
    }
}
