package com.bautruc.ecommerce.sitecontent.api.response;

import java.util.List;
import com.bautruc.ecommerce.sitecontent.application.HomepageContentSnapshot;

public record AdminHomepageResponse(
        List<HomepageMediaResponse> media,
        List<HomepageFeaturedProductResponse> featuredProducts,
        String sloganVi,
        String sloganEn
) {
    public static AdminHomepageResponse from(HomepageContentSnapshot snapshot) {
        return new AdminHomepageResponse(
                snapshot.media().stream().map(HomepageMediaResponse::from).toList(),
                snapshot.featuredProducts().stream().map(HomepageFeaturedProductResponse::from).toList(),
                snapshot.slogan().vi(),
                snapshot.slogan().en()
        );
    }
}
