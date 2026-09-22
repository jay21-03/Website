package com.bautruc.ecommerce.sitecontent.application;

import java.util.List;
import java.util.Map;

public record HomepageContentSnapshot(
        String sloganVi,
        String sloganEn,
        Map<String, String> copy,
        List<SiteMediaView> media,
        List<FeaturedProductItem> featuredProducts
) {
}
