package com.bautruc.ecommerce.sitecontent.application;

import java.util.List;

public record HomepageContentSnapshot(
        List<SiteMediaView> media,
        List<FeaturedProductItem> featuredProducts
) {
}
