package com.bautruc.ecommerce.sitecontent.api.response;

import java.util.List;
import java.util.Map;
import com.bautruc.ecommerce.sitecontent.application.HomepageContentSnapshot;

public record AdminHomepageResponse(
        String sloganVi,
        String sloganEn,
        Map<String, String> copy,
        List<HomepageMediaResponse> media,
        List<HomepageFeaturedProductResponse> featuredProducts
) {
    public static AdminHomepageResponse from(HomepageContentSnapshot snapshot) {
        return new AdminHomepageResponse(
                snapshot.sloganVi(),
                snapshot.sloganEn(),
                snapshot.copy(),
                snapshot.media().stream().map(HomepageMediaResponse::from).toList(),
                snapshot.featuredProducts().stream().map(HomepageFeaturedProductResponse::from).toList()
        );
    }
}
