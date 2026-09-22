package com.bautruc.ecommerce.sitecontent.api.response;

import java.util.List;
import java.util.Map;
import com.bautruc.ecommerce.sitecontent.application.HomepageContentSnapshot;
import com.bautruc.ecommerce.sitecontent.application.SiteMediaView;
import com.bautruc.ecommerce.sitecontent.domain.SiteMediaSlot;

public record HomepageResponse(
        String sloganVi,
        String sloganEn,
        Map<String, String> copy,
        String heroImageUrl,
        String storyImageUrl,
        List<HomepageMediaResponse> socialImages,
        List<HomepageFeaturedProductResponse> featuredProducts
) {
    public static HomepageResponse from(HomepageContentSnapshot snapshot) {
        return new HomepageResponse(
                snapshot.sloganVi(),
                snapshot.sloganEn(),
                snapshot.copy(),
                url(snapshot, SiteMediaSlot.HOME_HERO),
                url(snapshot, SiteMediaSlot.HOME_STORY),
                snapshot.media().stream()
                        .filter(item -> item.slot().name().startsWith("HOME_SOCIAL_"))
                        .map(HomepageMediaResponse::from)
                        .toList(),
                snapshot.featuredProducts().stream()
                        .map(HomepageFeaturedProductResponse::from)
                        .toList()
        );
    }

    private static String url(HomepageContentSnapshot snapshot, SiteMediaSlot slot) {
        return snapshot.media().stream()
                .filter(item -> item.slot() == slot)
                .findFirst()
                .map(SiteMediaView::url)
                .orElse(null);
    }
}
